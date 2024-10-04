package no.nav.paw.bekreftelse.api.topology

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.api.utils.buildStreamsLogger
import no.nav.paw.bekreftelse.api.utils.ignorertBekreftelseHendeleCounter
import no.nav.paw.bekreftelse.api.utils.lagreBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.api.utils.mottattBekreftelseHendelseKafkaCounter
import no.nav.paw.bekreftelse.api.utils.slettetBekreftelseHendelseCounter
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore

private val logger = buildStreamsLogger

fun KStream<Long, BekreftelseHendelse>.oppdaterBekreftelseHendelseState(
    stateStoreName: String,
    meterRegistry: MeterRegistry
): KStream<Long, BekreftelseHendelse> {
    val processor = {
        BekreftelseHendelseProcessor(stateStoreName, meterRegistry)
    }
    return process(processor, Named.`as`("bekreftelseHendelseProcessor"), stateStoreName)
}

class BekreftelseHendelseProcessor(
    private val stateStoreName: String,
    private val meterRegistry: MeterRegistry
) : Processor<Long, BekreftelseHendelse, Long, BekreftelseHendelse> {
    private var stateStore: KeyValueStore<Long, InternState>? = null
    private var context: ProcessorContext<Long, BekreftelseHendelse>? = null

    override fun init(context: ProcessorContext<Long, BekreftelseHendelse>) {
        super.init(context)
        this.context = context
        stateStore = context.getStateStore(stateStoreName)
    }

    // TODO Legg til metrics
    @WithSpan
    override fun process(record: Record<Long, BekreftelseHendelse>?) {
        val hendelse = record?.value() ?: return

        val internStateStore = requireNotNull(stateStore) { "Intern state store er ikke initiert" }

        meterRegistry.mottattBekreftelseHendelseKafkaCounter(hendelse.hendelseType)
        logger.debug("Mottok hendelse av type {}", hendelse.hendelseType)

        when (hendelse) {
            is BekreftelseTilgjengelig -> {
                internStateStore.processBekreftelseTilgjengelig(meterRegistry, hendelse)
            }

            is BekreftelseMeldingMottatt -> {
                internStateStore.processBekreftelseMeldingMottatt(meterRegistry, hendelse)
            }

            is PeriodeAvsluttet -> {
                internStateStore.processPeriodeAvsluttet(meterRegistry, hendelse)
            }

            else -> {
                processAnnenHendelse(meterRegistry, hendelse)
            }
        }
    }
}

@WithSpan(
    value = "processBekreftelseTilgjengelig",
    kind = SpanKind.INTERNAL
)
fun KeyValueStore<Long, InternState>.processBekreftelseTilgjengelig(
    meterRegistry: MeterRegistry,
    hendelse: BekreftelseTilgjengelig
) {
    val currentSpan = Span.current()
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "lagrer_bekreftelse")
    meterRegistry.lagreBekreftelseHendelseCounter(hendelse.hendelseType)
    val internState = get(hendelse.arbeidssoekerId)
    if (internState != null) {
        val originalSize = internState.tilgjendeligeBekreftelser.size
        val tilgjengeligeBekreftelser = internState.tilgjendeligeBekreftelser + hendelse
        val nySize = tilgjengeligeBekreftelser.size
        logger.debug("Lagrer bekreftelse i eksisterende state ({} -> {})", originalSize, nySize)
        put(hendelse.arbeidssoekerId, InternState(tilgjengeligeBekreftelser))
    } else {
        logger.debug("Lagrer bekreftelse i ny state ({} -> {})", 0, 1)
        put(hendelse.arbeidssoekerId, InternState(listOf(hendelse)))
    }
}

@WithSpan(
    value = "processBekreftelseMeldingMottatt",
    kind = SpanKind.INTERNAL
)
fun KeyValueStore<Long, InternState>.processBekreftelseMeldingMottatt(
    meterRegistry: MeterRegistry,
    hendelse: BekreftelseMeldingMottatt
) {
    val currentSpan = Span.current()
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)

    val internState = get(hendelse.arbeidssoekerId)
    if (internState != null) {
        val originalSize = internState.tilgjendeligeBekreftelser.size
        internState.tilgjendeligeBekreftelser
            .filterNot { it.bekreftelseId == hendelse.bekreftelseId }
            .let { bekreftelser ->
                if (bekreftelser.isEmpty()) {
                    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "sletter_intern_state")
                    meterRegistry.slettetBekreftelseHendelseCounter(hendelse.hendelseType)
                    logger.debug("Sletter all state ({} -> {})", originalSize, 0)
                    delete(hendelse.arbeidssoekerId)
                } else {
                    val nySize = bekreftelser.size
                    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "sletter_bekreftelser")
                    meterRegistry.slettetBekreftelseHendelseCounter(hendelse.hendelseType, nySize.toLong())
                    logger.debug("Sletter bekreftelser ({} -> {})", originalSize, nySize)
                    put(hendelse.arbeidssoekerId, InternState(bekreftelser))
                }
            }
    } else {
        logger.warn("Mottok bekreftelsesmelding, men det finnes ingen state for arbeidssøker")
    }
}

@WithSpan(
    value = "processPeriodeAvsluttet",
    kind = SpanKind.INTERNAL
)
fun KeyValueStore<Long, InternState>.processPeriodeAvsluttet(
    meterRegistry: MeterRegistry,
    hendelse: PeriodeAvsluttet
) {
    val currentSpan = Span.current()
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)

    val internState = get(hendelse.arbeidssoekerId)
    if (internState != null) {
        val originalSize = internState.tilgjendeligeBekreftelser.size
        internState.tilgjendeligeBekreftelser
            .filterNot { it.periodeId == hendelse.periodeId }
            .let { bekreftelser ->
                if (bekreftelser.isEmpty()) {
                    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "sletter_intern_state")
                    meterRegistry.slettetBekreftelseHendelseCounter(hendelse.hendelseType)
                    logger.debug("Sletter all state ({} -> {})", originalSize, 0)
                    delete(hendelse.arbeidssoekerId)
                } else {
                    val nySize = bekreftelser.size
                    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "sletter_bekreftelser")
                    meterRegistry.slettetBekreftelseHendelseCounter(hendelse.hendelseType, nySize.toLong())
                    logger.debug("Sletter bekreftelser for periode ({} -> {})", originalSize, nySize)
                    put(hendelse.arbeidssoekerId, InternState(bekreftelser))
                }
            }
    } else {
        logger.warn("Mottok melding om avsluttet periode, men det finnes ingen state for arbeidssøker")
    }
}

@WithSpan(
    value = "processAnnenHendelse",
    kind = SpanKind.INTERNAL
)
fun processAnnenHendelse(
    meterRegistry: MeterRegistry,
    hendelse: BekreftelseHendelse
) {
    val currentSpan = Span.current()
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "ignorerer_hendelse")
    meterRegistry.ignorertBekreftelseHendeleCounter(hendelse.hendelseType)
    logger.debug("Ignorerer hendelse av type {}", hendelse.hendelseType)
}

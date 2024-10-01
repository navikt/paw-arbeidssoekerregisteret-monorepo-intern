package no.nav.paw.bekreftelse.api.topology

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.model.InternState
import no.nav.paw.bekreftelse.api.utils.buildStreamsLogger
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
    stateStoreName: String, meterRegistry: MeterRegistry
): KStream<Long, BekreftelseHendelse> {
    val processor = {
        BekreftelseHendelseProcessor(stateStoreName, meterRegistry)
    }
    return process(processor, Named.`as`("bekreftelseHendelseProcessor"), stateStoreName)
}

class BekreftelseHendelseProcessor(
    private val stateStoreName: String, meterRegistry: MeterRegistry
) : Processor<Long, BekreftelseHendelse, Long, BekreftelseHendelse> {
    private var stateStore: KeyValueStore<Long, InternState>? = null
    private var context: ProcessorContext<Long, BekreftelseHendelse>? = null

    override fun init(context: ProcessorContext<Long, BekreftelseHendelse>?) {
        super.init(context)
        this.context = context
        stateStore = context?.getStateStore(stateStoreName)
    }

    // TODO Legg til metrics
    @WithSpan
    override fun process(record: Record<Long, BekreftelseHendelse>?) {
        val hendelse = record?.value() ?: return

        val internStateStore = requireNotNull(stateStore) { "Intern state store er ikke initiert" }

        when (hendelse) {
            is BekreftelseTilgjengelig -> {
                internStateStore.processBekreftelseTilgjengelig(hendelse)
            }

            is BekreftelseMeldingMottatt -> {
                internStateStore.processBekreftelseMeldingMottatt(hendelse)
            }

            is PeriodeAvsluttet -> {
                internStateStore.processPeriodeAvsluttet(hendelse)
            }

            else -> {
                processAnnenHendelse(hendelse)
            }
        }
    }
}

@WithSpan(
    value = "processBekreftelseTilgjengelig",
    kind = SpanKind.INTERNAL
)
fun KeyValueStore<Long, InternState>.processBekreftelseTilgjengelig(hendelse: BekreftelseTilgjengelig) {
    val currentSpan = Span.current()
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "lagrer_bekreftelse")
    val internState = get(hendelse.arbeidssoekerId)
    if (internState != null) {
        val tilgjengeligeBekreftelser = internState.tilgjendeligeBekreftelser + hendelse
        put(hendelse.arbeidssoekerId, InternState(tilgjengeligeBekreftelser))
    } else {
        put(hendelse.arbeidssoekerId, InternState(listOf(hendelse)))
    }
}

@WithSpan(
    value = "processBekreftelseMeldingMottatt",
    kind = SpanKind.INTERNAL
)
fun KeyValueStore<Long, InternState>.processBekreftelseMeldingMottatt(hendelse: BekreftelseMeldingMottatt) {
    val currentSpan = Span.current()
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
    val internState = get(hendelse.arbeidssoekerId)
    internState?.let { state ->
        state.tilgjendeligeBekreftelser
            .filterNot { it.bekreftelseId == hendelse.bekreftelseId }
            .let { bekreftelser ->
                if (bekreftelser.isEmpty()) {
                    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "sletter_intern_state")
                    delete(hendelse.arbeidssoekerId)
                } else {
                    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "sletter_bekreftelser")
                    put(hendelse.arbeidssoekerId, InternState(bekreftelser))
                }
            }
    }
}

@WithSpan(
    value = "processPeriodeAvsluttet",
    kind = SpanKind.INTERNAL
)
fun KeyValueStore<Long, InternState>.processPeriodeAvsluttet(hendelse: PeriodeAvsluttet) {
    val currentSpan = Span.current()
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
    get(hendelse.arbeidssoekerId)?.let { state ->
        state.tilgjendeligeBekreftelser.filterNot { it.periodeId == hendelse.periodeId }
            .let { bekreftelser ->
                if (bekreftelser.isEmpty()) {
                    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "sletter_intern_state")
                    delete(hendelse.arbeidssoekerId)
                } else {
                    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "sletter_bekreftelser")
                    put(hendelse.arbeidssoekerId, InternState(bekreftelser))
                }
            }
    }
}

@WithSpan(
    value = "processAnnenHendelse",
    kind = SpanKind.INTERNAL
)
fun processAnnenHendelse(hendelse: BekreftelseHendelse) {
    val currentSpan = Span.current()
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.hendelse.type", hendelse.hendelseType)
    currentSpan.setAttribute("paw.arbeidssoekerregisteret.aksjon", "ignorerer_hendelse")
    logger.debug("Ignorerer hendelse av type {}", hendelse.hendelseType)
}

package no.nav.paw.rapportering.api.kafka

import no.nav.paw.rapportering.internehendelser.PeriodeAvsluttet
import no.nav.paw.rapportering.internehendelser.RapporteringTilgjengelig
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import no.nav.paw.rapportering.internehendelser.RapporteringsMeldingMottatt
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.processor.api.Record

fun KStream<Long, RapporteringsHendelse>.oppdaterRapporteringHendelseState(
    rapporteringStateStoreName: String
): KStream<Long, RapporteringsHendelse> {
    val processor = {
        RapporteringHendelseProcessor(rapporteringStateStoreName)
    }
    return process(processor, Named.`as`("rapporteringHendelseProcessor"), rapporteringStateStoreName)
}

class RapporteringHendelseProcessor(
    private val stateStoreName: String,
): Processor<Long, RapporteringsHendelse, Long, RapporteringsHendelse> {
    private var stateStore: KeyValueStore<Long, RapporteringTilgjengeligState>? = null
    private var context: ProcessorContext<Long, RapporteringsHendelse>? = null

    override fun init(context: ProcessorContext<Long, RapporteringsHendelse>?) {
        super.init(context)
        this.context = context
        stateStore = context?.getStateStore(stateStoreName)
    }

    override fun process(record: Record<Long, RapporteringsHendelse>?) {
        val value = record?.value() ?: return
        val hendelseStore = requireNotNull(stateStore) { "State store is not initialized" }
        when (value) {
            is RapporteringTilgjengelig -> {
                hendelseStore.get(value.arbeidssoekerId)?.let {
                    hendelseStore.put(value.arbeidssoekerId, RapporteringTilgjengeligState(it.rapporteringer.plus(value)))
                } ?: hendelseStore.put(value.arbeidssoekerId, RapporteringTilgjengeligState(listOf(value)))
            }
            is RapporteringsMeldingMottatt -> {
                hendelseStore.get(value.arbeidssoekerId)?.let { state ->
                    state.rapporteringer
                        .filterNot { it.rapporteringsId == value.rapporteringsId }
                        .let { rapporteringer ->
                            if (rapporteringer.isEmpty()) hendelseStore.delete(value.arbeidssoekerId)
                            else hendelseStore.put(value.arbeidssoekerId, RapporteringTilgjengeligState(rapporteringer))
                        }
                }
            }
            is PeriodeAvsluttet -> {
                hendelseStore.get(value.arbeidssoekerId)?.let {
                    hendelseStore.delete(value.arbeidssoekerId)
                }
            }
        }
    }
}
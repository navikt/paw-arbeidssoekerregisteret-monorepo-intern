package no.nav.paw.bekreftelse.api.kafka

import no.nav.paw.bekreftelse.api.model.InternState
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

fun KStream<Long, BekreftelseHendelse>.oppdaterBekreftelseHendelseState(
    stateStoreName: String
): KStream<Long, BekreftelseHendelse> {
    val processor = {
        BekreftelseHendelseProcessor(stateStoreName)
    }
    return process(processor, Named.`as`("bekreftelseHendelseProcessor"), stateStoreName)
}

class BekreftelseHendelseProcessor(
    private val stateStoreName: String,
) : Processor<Long, BekreftelseHendelse, Long, BekreftelseHendelse> {
    private var stateStore: KeyValueStore<Long, InternState>? = null
    private var context: ProcessorContext<Long, BekreftelseHendelse>? = null

    override fun init(context: ProcessorContext<Long, BekreftelseHendelse>?) {
        super.init(context)
        this.context = context
        stateStore = context?.getStateStore(stateStoreName)
    }

    override fun process(record: Record<Long, BekreftelseHendelse>?) {
        val value = record?.value() ?: return
        val hendelseStore = requireNotNull(stateStore) { "State store is not initialized" }
        when (value) {
            is BekreftelseTilgjengelig -> {
                hendelseStore.get(value.arbeidssoekerId)?.let {
                    hendelseStore.put(
                        value.arbeidssoekerId,
                        InternState(it.tilgjendeligeBekreftelser.plus(value))
                    )
                } ?: hendelseStore.put(value.arbeidssoekerId, InternState(listOf(value)))
            }

            is BekreftelseMeldingMottatt -> {
                hendelseStore.get(value.arbeidssoekerId)?.let { state ->
                    state.tilgjendeligeBekreftelser
                        .filterNot { it.bekreftelseId == value.bekreftelseId }
                        .let { bekreftelser ->
                            if (bekreftelser.isEmpty()) hendelseStore.delete(value.arbeidssoekerId)
                            else hendelseStore.put(value.arbeidssoekerId, InternState(bekreftelser))
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
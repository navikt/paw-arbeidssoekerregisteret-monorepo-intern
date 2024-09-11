package no.nav.paw.bekretelsetjeneste

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.config.kafka.streams.mapWithContext
import no.nav.paw.bekretelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekretelsetjeneste.tilstand.initTilstand
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*


context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.processPeriodeTopic() {
    stream<Long, Periode>(periodeTopic)
        .mapWithContext("lagreEllerSlettPeriode", stateStoreName) { periode ->
            val keyValueStore: KeyValueStore<UUID, InternTilstand> = getStateStore(stateStoreName)
            val currentState = keyValueStore[periode.id]
            val (arbeidsoekerId, kafkaKey) = currentState?.let { it.periode.arbeidsoekerId to it.periode.recordKey } ?:
                    kafkaKeyFunction(periode.identitetsnummer).let { it.id to it.key}
            when {
                currentState == null && periode.avsluttet() -> Action.DoNothing
                periode.avsluttet() -> Action.DeleteStateAndEmit(arbeidsoekerId, periode)
                currentState == null -> Action.UpdateState(initTilstand(id = arbeidsoekerId, key = kafkaKey, periode = periode))
                else -> Action.DoNothing
            }
        }
        .genericProcess<Long, Action, Long, BekreftelseHendelse>(
            name = "executeAction",
            punctuation = null,
            stateStoreNames = arrayOf(stateStoreName)
        ) { record ->
            val keyValueStore: KeyValueStore<UUID, InternTilstand> = getStateStore(stateStoreName)
            when (val action = record.value()) {
                is Action.DeleteStateAndEmit -> {
                    forward(
                        record.withValue(
                            PeriodeAvsluttet(
                                UUID.randomUUID(),
                                action.periode.id,
                                action.arbeidsoekerId
                            ) as BekreftelseHendelse
                        )
                    )
                    keyValueStore.delete(action.periode.id)
                }

                Action.DoNothing -> {}
                is Action.UpdateState -> keyValueStore.put(action.state.periode.periodeId, action.state)
            }
        }.to(bekreftelseHendelseloggTopic, Produced.with(Serdes.Long(), bekreftelseHendelseSerde))
}

fun Periode.avsluttet(): Boolean = avsluttet != null

sealed interface Action {
    data object DoNothing : Action
    data class DeleteStateAndEmit(val arbeidsoekerId: Long, val periode: Periode) : Action
    data class UpdateState(val state: InternTilstand) : Action
}

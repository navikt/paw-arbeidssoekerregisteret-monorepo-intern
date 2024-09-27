package no.nav.paw.bekreftelsetjeneste

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelsetjeneste.context.ApplicationContext
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.initTilstand
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.config.kafka.streams.mapWithContext
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.*

fun StreamsBuilder.processPeriodeTopic(applicationContext: ApplicationContext) {
    with(applicationContext.applicationConfig) {
        val kafkaKeysClient = applicationContext.kafkaKeysClient

        stream<Long, Periode>(kafkaTopology.periodeTopic)
            .mapWithContext<Long, Periode, Action>(
                "lagreEllerSlettPeriode",
                kafkaTopology.internStateStoreName
            ) { periode ->
                val keyValueStore: KeyValueStore<UUID, InternTilstand> =
                    getStateStore(kafkaTopology.internStateStoreName)
                val currentState = keyValueStore[periode.id]
                val (arbeidsoekerId, kafkaKey) = currentState?.let { it.periode.arbeidsoekerId to it.periode.recordKey }
                    ?: kafkaKeysClient.getIdAndKeyBlocking(periode.identitetsnummer).let { it.id to it.key }
                when {
                    currentState == null && periode.avsluttet() -> Action.DoNothing
                    periode.avsluttet() -> Action.DeleteStateAndEmit(arbeidsoekerId, periode)
                    currentState == null -> Action.UpdateState(
                        initTilstand(
                            id = arbeidsoekerId,
                            key = kafkaKey,
                            periode = periode
                        )
                    )

                    else -> Action.DoNothing
                }
            }
            .genericProcess<Long, Action, Long, BekreftelseHendelse>(
                name = "executeAction",
                punctuation = null,
                stateStoreNames = arrayOf(kafkaTopology.internStateStoreName)
            ) { record ->
                val keyValueStore: KeyValueStore<UUID, InternTilstand> =
                    getStateStore(kafkaTopology.internStateStoreName)
                when (val action = record.value()) {
                    is Action.DeleteStateAndEmit -> {
                        forward(
                            record.withValue(
                                PeriodeAvsluttet(
                                    hendelseId = UUID.randomUUID(),
                                    periodeId = action.periode.id,
                                    arbeidssoekerId = action.arbeidsoekerId,
                                    hendelseTidspunkt = Instant.now()
                                ) as BekreftelseHendelse
                            )
                        )
                        keyValueStore.delete(action.periode.id)
                    }

                    Action.DoNothing -> {}
                    is Action.UpdateState -> keyValueStore.put(action.state.periode.periodeId, action.state)
                }
            }.to(kafkaTopology.bekreftelseHendelsesloggTopic, Produced.with(Serdes.Long(), BekreftelseHendelseSerde()))
    }
}

fun Periode.avsluttet(): Boolean = avsluttet != null

sealed interface Action {
    data object DoNothing : Action
    data class DeleteStateAndEmit(val arbeidsoekerId: Long, val periode: Periode) : Action
    data class UpdateState(val state: InternTilstand) : Action
}

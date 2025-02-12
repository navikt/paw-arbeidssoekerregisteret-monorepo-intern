package no.nav.paw.bekreftelsetjeneste.topology

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.opprettBekreftelseTilstand
import no.nav.paw.kafka.processor.genericProcess
import no.nav.paw.kafka.processor.mapWithContext
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.*
import kotlin.jvm.optionals.getOrNull

fun StreamsBuilder.buildPeriodeStream(
    applicationConfig: ApplicationConfig,
    kafaKeysClient: KafkaKeysClient
) {
    with(applicationConfig.kafkaTopology) {
        stream<Long, Periode>(periodeTopic)
            .mapWithContext<Long, Periode, Action>(
                "lagreEllerSlettPeriode",
                internStateStoreName
            ) { periode ->
                val keyValueStore: KeyValueStore<UUID, BekreftelseTilstand> =
                    getStateStore(internStateStoreName)
                val currentState = keyValueStore[periode.id]
                val (arbeidsoekerId, kafkaKey) = currentState?.let { it.periode.arbeidsoekerId to it.periode.recordKey }
                    ?: kafaKeysClient.getIdAndKeyBlocking(periode.identitetsnummer).let { it.id to it.key }
                when {
                    currentState == null && periode.avsluttet() -> Action.DoNothing
                    periode.avsluttet() -> Action.DeleteStateAndEmit(arbeidsoekerId, periode)
                    currentState == null -> Action.UpdateState(
                        opprettBekreftelseTilstand(
                            kafkaPartition = requireNotNull(recordMetadata().getOrNull()?.partition()) { "Forventer at kafka.partition er satt"},
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
                stateStoreNames = arrayOf(internStateStoreName)
            ) { record ->
                val keyValueStore: KeyValueStore<UUID, BekreftelseTilstand> =
                    getStateStore(internStateStoreName)
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
                            ).withTimestamp(Instant.now().toEpochMilli())
                        )
                        keyValueStore.delete(action.periode.id)
                    }

                    Action.DoNothing -> {}
                    is Action.UpdateState -> keyValueStore.put(action.state.periode.periodeId, action.state)
                }
            }.to(bekreftelseHendelseloggTopic, Produced.with(Serdes.Long(), BekreftelseHendelseSerde()))
    }
}

fun Periode.avsluttet(): Boolean = avsluttet != null

sealed interface Action {
    data object DoNothing : Action
    data class DeleteStateAndEmit(val arbeidsoekerId: Long, val periode: Periode) : Action
    data class UpdateState(val state: BekreftelseTilstand) : Action
}

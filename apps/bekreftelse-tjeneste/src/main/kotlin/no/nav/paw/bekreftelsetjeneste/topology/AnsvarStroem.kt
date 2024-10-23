package no.nav.paw.bekreftelsetjeneste.topology

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.ansvar.v1.AnsvarEndret
import no.nav.paw.bekreftelse.ansvar.v1.vo.AvslutterAnsvar
import no.nav.paw.bekreftelse.ansvar.v1.vo.TarAnsvar
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelsetjeneste.ansvar.*
import no.nav.paw.bekreftelsetjeneste.config.KafkaTopologyConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.config.kafka.streams.mapNonNull
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*

fun StreamsBuilder.byggAnsvarsStroem(
    registry: PrometheusMeterRegistry,
    kafkaTopologyConfig: KafkaTopologyConfig,
    bekreftelseHendelseSerde: BekreftelseHendelseSerde
) {
    stream<Long, AnsvarEndret>(kafkaTopologyConfig.ansvarsTopic)
        .peek { _, message -> count(registry, message) }
        .mapNonNull(
            name = "endre_ansvar",
            kafkaTopologyConfig.ansvarStateStoreName,
            kafkaTopologyConfig.internStateStoreName
        ) { message ->
            val internStateStore =
                getStateStore<KeyValueStore<UUID, InternTilstand>>(kafkaTopologyConfig.internStateStoreName)
            val ansvarStateStore = getStateStore<KeyValueStore<UUID, Ansvar>>(kafkaTopologyConfig.ansvarStateStoreName)
            val internTilstand = internStateStore[message.periodeId]
            val ansvar = ansvarStateStore[message.periodeId]
            haandterAnsvarEndret(
                tilstand = internTilstand,
                ansvar = ansvar,
                ansvarEndret = message
            ).map { handling ->
                when (handling) {
                    is SendHendelse -> handling.hendelse
                    is SkrivAnsvar -> ansvarStateStore.put(handling.id, handling.value)
                    is SlettAnsvar -> ansvarStateStore.delete(handling.id)
                }
            }.filterIsInstance<BekreftelseHendelse>()
        }
        .flatMapValues { _, value -> value }
        .to(
            kafkaTopologyConfig.bekreftelseHendelseloggTopic,
            Produced.with(Serdes.Long(), bekreftelseHendelseSerde)
        )
}

fun count(
    registry: PrometheusMeterRegistry,
    message: AnsvarEndret
) {
    val action = when (message.handling) {
        is TarAnsvar -> "tar_ansvar"
        is AvslutterAnsvar -> "avslutter_ansvar"
        else -> "ukjent"
    }
    val bekreftelsesloesning = message.bekreftelsesloesning
    registry.counter(
        "paw_bekreftelse_ansvar_endret",
        Tags.of(
            Tag.of("bekreftelsesloesing", bekreftelsesloesning.name),
            Tag.of("handling", action)
        )
    )
}

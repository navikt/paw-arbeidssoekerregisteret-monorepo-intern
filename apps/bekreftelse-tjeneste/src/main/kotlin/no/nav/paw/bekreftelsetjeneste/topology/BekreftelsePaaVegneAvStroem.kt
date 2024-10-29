package no.nav.paw.bekreftelsetjeneste.topology

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.bekreftelsetjeneste.paavegneav.*
import no.nav.paw.bekreftelsetjeneste.config.KafkaTopologyConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.config.kafka.streams.mapNonNull
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.*

fun StreamsBuilder.byggBekreftelsePaaVegneAvStroem(
    registry: PrometheusMeterRegistry,
    kafkaTopologyConfig: KafkaTopologyConfig,
    bekreftelseHendelseSerde: BekreftelseHendelseSerde
) {
    stream<Long, PaaVegneAv>(kafkaTopologyConfig.bekreftelsePaaVegneAvTopic)
        .peek { _, message -> count(registry, message) }
        .mapNonNull(
            name = "bekreftelse_paa_vegne_av_stroem",
            kafkaTopologyConfig.bekreftelsePaaVegneAvStateStoreName,
            kafkaTopologyConfig.internStateStoreName
        ) { message ->
            val bekreftelseTilstandStateStore =
                getStateStore<KeyValueStore<UUID, BekreftelseTilstand>>(kafkaTopologyConfig.internStateStoreName)
            val paaVegneAvTilstandStateStore = getStateStore<KeyValueStore<UUID, PaaVegneAvTilstand>>(kafkaTopologyConfig.bekreftelsePaaVegneAvStateStoreName)
            val bekreftelseTilstand = bekreftelseTilstandStateStore[message.periodeId]
            val paaVegneAvTilstand = paaVegneAvTilstandStateStore[message.periodeId]
            haandterBekreftelsePaaVegneAvEndret(
                wallclock = WallClock(Instant.now()),
                bekreftelseTilstand = bekreftelseTilstand,
                paaVegneAvTilstand = paaVegneAvTilstand,
                paaVegneAvHendelse = message
            ).map { handling ->
                when (handling) {
                    is SendHendelse -> handling.hendelse
                    is SkrivPaaVegneAvTilstand -> paaVegneAvTilstandStateStore.put(handling.id, handling.value)
                    is SlettPaaVegneAvTilstand -> paaVegneAvTilstandStateStore.delete(handling.id)
                    is SkrivBekreftelseTilstand -> bekreftelseTilstandStateStore.put(handling.id, handling.value)
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
    message: PaaVegneAv
) {
    val action = when (message.handling) {
        is Start -> "start"
        is Stopp -> "stopp"
        else -> "ukjent"
    }
    val bekreftelsesloesning = message.bekreftelsesloesning
    registry.counter(
        "paw_bekreftelse_pa_vegne_av",
        Tags.of(
            Tag.of("bekreftelsesloesing", bekreftelsesloesning.name),
            Tag.of("handling", action)
        )
    )
}

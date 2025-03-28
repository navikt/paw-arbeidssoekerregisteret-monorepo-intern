package no.nav.paw.bekreftelsetjeneste.topology

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.config.KafkaTopologyConfig
import no.nav.paw.bekreftelsetjeneste.metrics.tellBekreftelseUtgaaendeHendelse
import no.nav.paw.bekreftelsetjeneste.metrics.tellPaVegneAv
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.paavegneav.SendHendelse
import no.nav.paw.bekreftelsetjeneste.paavegneav.SkrivBekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.paavegneav.SkrivPaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.paavegneav.SlettPaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.paavegneav.haandterBekreftelsePaaVegneAvEndret
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.kafka.processor.mapNonNull
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.*

fun StreamsBuilder.byggBekreftelsePaaVegneAvStroem(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    registry: PrometheusMeterRegistry,
    kafkaTopologyConfig: KafkaTopologyConfig,
    bekreftelseHendelseSerde: BekreftelseHendelseSerde
) {
    stream<Long, PaaVegneAv>(kafkaTopologyConfig.bekreftelsePaaVegneAvTopic)
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
            registry.tellPaVegneAv(
                paaVegneAv = message,
                periodeFunnet = bekreftelseTilstand != null,
                ansvarlige = paaVegneAvTilstand?.paaVegneAvList?.map { it.loesning } ?: emptyList()
            )
            haandterBekreftelsePaaVegneAvEndret(
                wallclock = WallClock(Instant.ofEpochMilli(this.currentSystemTimeMs())),
                bekreftelseKonfigurasjon = bekreftelseKonfigurasjon,
                bekreftelseTilstand = bekreftelseTilstand,
                paaVegneAvTilstand = paaVegneAvTilstand,
                paaVegneAvHendelse = message
            ).mapNotNull { handling ->
                when (handling) {
                    is SendHendelse -> handling.hendelse
                    is SkrivPaaVegneAvTilstand -> paaVegneAvTilstandStateStore.put(handling.id, handling.value)
                    is SlettPaaVegneAvTilstand -> paaVegneAvTilstandStateStore.delete(handling.id)
                    is SkrivBekreftelseTilstand -> bekreftelseTilstandStateStore.put(handling.id, handling.value)
                }
            }.filterIsInstance<BekreftelseHendelse>()
        }
        .flatMapValues { _, value -> value }
        .peek { _, value -> registry.tellBekreftelseUtgaaendeHendelse(value) }
        .to(
            kafkaTopologyConfig.bekreftelseHendelseloggTopic,
            Produced.with(Serdes.Long(), bekreftelseHendelseSerde)
        )
}

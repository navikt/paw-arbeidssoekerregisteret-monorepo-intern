package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.avsluttPeriode
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafka.filtrer
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafka.genererTilstander
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafka.lastTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stoppet
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDbKeyValueBytesStoreSupplier
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

const val kafkaKonfigurasjonsfil = "kafka_konfigurasjon.toml"

typealias Hendelse = SpecificRecord

fun main() {
    val streamLogger = LoggerFactory.getLogger("App")
    streamLogger.info("Starter applikasjon...")

    val kafkaKonfigurasjon = lastKonfigurasjon<KafkaKonfigurasjon>(kafkaKonfigurasjonsfil)

    val tilstandsSerde: Serde<Tilstand> = TilstandSerde()

    val dbNavn = kafkaKonfigurasjon.streamKonfigurasjon.tilstandsDatabase
    val builder = StreamsBuilder()
    builder.addStateStore(
        KeyValueStoreBuilder(
            RocksDbKeyValueBytesStoreSupplier(dbNavn, false),
            Serdes.String(),
            tilstandsSerde,
            Time.SYSTEM
        )
    )
    val topology = topology(
        builder,
        dbNavn,
        kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic,
        kafkaKonfigurasjon.streamKonfigurasjon.periodeTopic
    )

    val kafkaStreams = KafkaStreams(topology, StreamsConfig(kafkaKonfigurasjon.properties))
    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        streamLogger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
    kafkaStreams.start()
    streamLogger.info("KafkaStreams tilstand: ${kafkaStreams.state()}")
    val avslutt = LinkedBlockingQueue<Unit>(1)
    Runtime.getRuntime().addShutdownHook(Thread {
        streamLogger.info("Avslutter...")
        kafkaStreams.close()
        avslutt.put(Unit)
    })
    while (avslutt.poll(30, TimeUnit.SECONDS) == null) {
        streamLogger.info("KafkaStreams tilstand: ${kafkaStreams.state()}")
    }
    streamLogger.info("Avsluttet")
    streamLogger.info("KafkaStreams tilstand: ${kafkaStreams.state()}")
}

fun topology(
    builder: StreamsBuilder,
    dbNavn: String,
    innTopic: String,
    utTopic: String
): Topology {
    val strøm: KStream<Long, SpecificRecord> = builder.stream(innTopic)
    strøm
        .lastTilstand(dbNavn)
        .filtrer(::ignorerDuplikatStartOgStopp)
        .genererTilstander(::genererTilstander)
    return builder.build()
}

fun genererTilstander(internTilstandOgHendelse: InternTilstandOgHendelse): InternTilstandOgApiTilstander {
    val (tilstand, hendelse) = internTilstandOgHendelse
    when {
        hendelse is Startet -> opprettPeriode(hendelse)
        hendelse is Stoppet -> tilstand.avsluttPeriode(hendelse)
        hendelse is SituasjonMottat -> tilstand.situsjonMottatt(hendelse)
        else -> throw IllegalStateException("Uventet hendelse: $hendelse")
    }
}




fun opprettPeriode(hendelse: Startet) {
    TODO("Not yet implemented")
}

fun Tilstand?.situsjonMottatt(hendelse: SituasjonMottat): InternTilstandOgApiTilstander {
    TODO("Not yet implemented")
}

fun ignorerDuplikatStartOgStopp(tilstand: Tilstand?, hendelse: Hendelse): Boolean =
    when(tilstand?.gjeldeneTilstand) {
        null -> hendelse.erIkkeEnAv<Stoppet, SituasjonMottat>()
        GjeldeneTilstand.STARTET -> hendelse.erIkke<Startet>()
        GjeldeneTilstand.STOPPET -> hendelse.erIkke<Stoppet>()
        GjeldeneTilstand.AVVIST -> hendelse.erIkkeEnAv<Stoppet, SituasjonMottat>()
    }

inline fun <reified A: Hendelse> Hendelse.erIkke(): Boolean = this !is A
inline fun <reified A: Hendelse, reified B: Hendelse> Hendelse.erIkkeEnAv(): Boolean = this !is A && this !is B


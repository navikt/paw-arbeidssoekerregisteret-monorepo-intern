package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
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

    val tilstandSerde: Serde<Tilstand> = TilstandSerde()

    val dbNavn = kafkaKonfigurasjon.streamKonfigurasjon.tilstandsDatabase
    val builder = StreamsBuilder()
    builder.addStateStore(
        KeyValueStoreBuilder(
            RocksDbKeyValueBytesStoreSupplier(dbNavn, false),
            Serdes.Long(),
            tilstandSerde,
            Time.SYSTEM
        )
    )
    val topology = topology(
        builder = builder,
        dbNavn = dbNavn,
        innTopic = kafkaKonfigurasjon.streamKonfigurasjon.eventlogTopic,
        periodeTopic = kafkaKonfigurasjon.streamKonfigurasjon.periodeTopic,
        situasjonTopic = kafkaKonfigurasjon.streamKonfigurasjon.situasjonTopic
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


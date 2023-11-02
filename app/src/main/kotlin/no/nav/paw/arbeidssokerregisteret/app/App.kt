package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.app.config.KafkaKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.app.config.SchemaRegistryKonfigurasjon
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.*
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.Operasjon.OPPRETT_ELLER_OPPDATER
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.Operasjon.SLETT
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Start
import no.nav.paw.arbeidssokerregisteret.intern.v1.Stopp
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

const val kafkaKonfigurasjonsfil = "kafka_konfigurasjon.toml"

fun main() {
    val streamLogger = LoggerFactory.getLogger("App")
    streamLogger.info("Starter applikasjon...")

    val kafkaKonfigurasjon = lastKonfigurasjon<KafkaKonfigurasjon>(kafkaKonfigurasjonsfil)

    val periodeSerde: Serde<PeriodeTilstandV1> = lagSpecificAvroSerde(kafkaKonfigurasjon.schemaRegistryKonfigurasjon)

    val dbNavn = kafkaKonfigurasjon.streamKonfigurasjon.tilstandsDatabase
    val builder = StreamsBuilder()
    builder.addStateStore(
        KeyValueStoreBuilder(
            RocksDbKeyValueBytesStoreSupplier(dbNavn, false),
            Serdes.String(),
            periodeSerde,
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
    avslutt.take()
    streamLogger.info("Avsluttet")
}

fun topology(
    builder: StreamsBuilder,
    dbNavn: String,
    innTopic: String,
    utTopic: String
): Topology {
    val strøm: KStream<String, Hendelse> = builder.stream(innTopic)
    strøm
        .filtrer(dbNavn) {
            when (hendelse.endring) {
                is Start -> inkluderDersomIkkeRegistrertArbeidssøker()
                is Stopp -> inkluderDersomRegistrertArbeidssøker()
                else -> inkluder
            }
        }
        .opprettEllerOppdaterPeriode(dbNavn) {
            when (hendelse.endring) {
                is Start -> startPeriode(hendelse)
                is Stopp -> avsluttPeriode(hendelse.metadata.tidspunkt)
                else -> ingenEndring()
            }
        }
        .oppdaterLagretPeriode(dbNavn) { periode ->
            when (periode.erAvsluttet()) {
                true -> SLETT
                false -> OPPRETT_ELLER_OPPDATER
            }
        }
        .to(utTopic)
    return builder.build()
}

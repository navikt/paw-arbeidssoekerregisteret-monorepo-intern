package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.app.config.KafkaSourceConfig
import no.nav.paw.arbeidssokerregisteret.app.config.SchemaRegistryConfig
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.filtererDuplikateStartStoppEventer
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

fun main() {
    val kildeConfig = KafkaSourceConfig(System.getenv())

    val streamLogger = LoggerFactory.getLogger("App")
    streamLogger.info("Starter stream")
    val streamsConfig = StreamsConfig(kildeConfig.properties)
    val schemaRegistryConfig = SchemaRegistryConfig(System.getenv())

    val hendelsesSerde: Serde<SpecificRecord> = lagSpecificAvroSerde(schemaRegistryConfig)
    val tilstandsSerde: Serde<PeriodeTilstandV1> = lagSpecificAvroSerde(schemaRegistryConfig)
    val produsent: Produced<String, SpecificRecord> = Produced.with(Serdes.String(), hendelsesSerde)

    val dbNavn = "tilstandsDb"
    val (builder, strøm) = konfigurerKafkaStrøm(
        hendelseLog = kildeConfig.eventlogTopic,
        tilstandSerde = tilstandsSerde,
        hendelseSerde = hendelsesSerde,
        dbNavn = dbNavn
    )
    strøm
        .filtererDuplikateStartStoppEventer(dbNavn)
        .to("output", produsent)


    val kafkaStreams = KafkaStreams(builder.build(), streamsConfig)
    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        streamLogger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
    kafkaStreams.start()
    Thread.sleep(Long.MAX_VALUE)
}


fun StartV1.periodeTilstand() = PeriodeTilstandV1(id, personNummer, timestamp)

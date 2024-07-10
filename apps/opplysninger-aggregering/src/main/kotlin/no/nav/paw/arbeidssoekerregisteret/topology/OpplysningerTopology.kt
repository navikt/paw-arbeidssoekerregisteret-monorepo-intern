package no.nav.paw.arbeidssoekerregisteret.topology

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.config.tellAntallMottatteOpplysninger
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

context(ApplicationContext)
fun buildOpplysningerTopology(
    meterRegistry: MeterRegistry
): Topology = StreamsBuilder().apply {
    logger.info("Oppretter KStream for opplysninger om arbeidssøker")
    val kafkaStreamsProperties = properties.kafkaStreams

    this.stream<Long, OpplysningerOmArbeidssoeker>(kafkaStreamsProperties.opplysningerTopic)
        .peek { key, _ ->
            logger.debug("Mottok event på {} med key {}", kafkaStreamsProperties.opplysningerTopic, key)
            meterRegistry.tellAntallMottatteOpplysninger()
        }
}.build()

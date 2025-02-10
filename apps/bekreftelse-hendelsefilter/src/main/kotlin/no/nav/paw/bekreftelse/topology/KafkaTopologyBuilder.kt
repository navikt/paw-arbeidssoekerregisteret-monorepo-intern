package no.nav.paw.bekreftelse.topology

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import no.nav.paw.bekreftelse.config.ApplicationConfig
import no.nav.paw.bekreftelse.config.ApplicationIdSuffix
import no.nav.paw.bekreftelse.config.bekreftelseApplicationIdSuffix
import no.nav.paw.bekreftelse.config.bekreftelsePaaVegneAvApplicationIdSuffix
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.kafka.processor.mapRecord
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.utils.buildApplicationLogger
import no.nav.paw.kafka.processor.mapNonNull
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

private val logger = buildApplicationLogger

fun buildKafkaTopologyList(applicationConfig: ApplicationConfig): List<Pair<ApplicationIdSuffix, Topology>> =
    applicationConfig.bekreftelseKlienter.flatMap { bekreftelseKlient ->
        listOf(
            bekreftelseKlient.bekreftelseApplicationIdSuffix to buildKafkaTopology<Bekreftelse>(
                bekreftelsesloesning = bekreftelseKlient.bekreftelsesloesning,
                sourceTopic = bekreftelseKlient.bekreftelseSourceTopic,
                targetTopic = applicationConfig.kafkaTopology.bekreftelseTargetTopic,
                hentLoesningFraMelding = { it.bekreftelsesloesning.name }
            ),
            bekreftelseKlient.bekreftelsePaaVegneAvApplicationIdSuffix to buildKafkaTopology<PaaVegneAv>(
                bekreftelsesloesning = bekreftelseKlient.bekreftelsesloesning,
                sourceTopic = bekreftelseKlient.paaVegneAvSourceTopic,
                targetTopic = applicationConfig.kafkaTopology.bekreftelsePaaVegneAvTargetTopic,
                hentLoesningFraMelding = { it.bekreftelsesloesning.name }
            )
        )
    }

fun <T: SpecificRecord> buildKafkaTopology(
    bekreftelsesloesning: String,
    sourceTopic: String,
    targetTopic: String,
    hentLoesningFraMelding: (T) -> String
): Topology = StreamsBuilder().apply {
    stream<Long, T>(sourceTopic)
        .peek { _, _ -> logger.debug("Mottok melding på topic {}", sourceTopic) }
        .mapNonNull(name = "verifiser_bekreftelseloesning") { value ->
            val loesningFraMelding = hentLoesningFraMelding(value)
            with(Span.current()) {
                val gyldig = loesningFraMelding.equals(bekreftelsesloesning, ignoreCase = true)
                val attributes = Attributes.of(
                    AttributeKey.stringKey("domain"), "bekreftelse",
                    AttributeKey.stringKey("acl_bekreftelsesloesning"), bekreftelsesloesning,
                    AttributeKey.stringKey("record_bekreftelsesloesning"), loesningFraMelding,
                    AttributeKey.booleanKey("gyldig_loesning"), gyldig
                )
                if (gyldig) {
                    addEvent("ok", attributes)
                    value
                } else {
                    addEvent("error", attributes)
                    setStatus(StatusCode.ERROR, "Bekreftelsesløsning fra melding matcher ikke forventet løsning")
                    logger.warn("Meldingens bekreftelsesløsning '$loesningFraMelding' matcher ikke forventet løsning '$bekreftelsesloesning'. Dropper melding.")
                    null
                }
            }
        }
        .mapRecord(name = "add_source_header") { record ->
            val headers = record.headers()
            val updatedHeaders = headers.add("source", bekreftelsesloesning.toByteArray())
            record.withHeaders(updatedHeaders)
        }
        .to(targetTopic)
}.build()

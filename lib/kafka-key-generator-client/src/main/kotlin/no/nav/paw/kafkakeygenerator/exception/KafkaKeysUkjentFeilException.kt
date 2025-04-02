package no.nav.paw.kafkakeygenerator.exception

import io.ktor.http.HttpStatusCode
import java.net.URI

private val KAFKA_KEYS_UNKNOWN_ERROR = URI.create("urn:paw:kafka-keys:ukjent-feil")

class KafkaKeysUkjentFeilException(
    override val status: HttpStatusCode,
    val response: String
) : KafkaKeysResponseException(status, KAFKA_KEYS_UNKNOWN_ERROR, "Det oppsto en feil ved henting av data, response=$response")
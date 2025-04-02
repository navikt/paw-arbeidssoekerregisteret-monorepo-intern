package no.nav.paw.kafkakeygenerator.exception

import io.ktor.http.HttpStatusCode
import java.net.URI

private val KAFKA_KEYS_NOT_FOUND_ERROR = URI.create("urn:paw:kafka-keys:not-found")

class KafkaKeysNotFoundException(
    val response: String
) : KafkaKeysResponseException(HttpStatusCode.NotFound, KAFKA_KEYS_NOT_FOUND_ERROR, "Fant ikke data for ident, response=$response")
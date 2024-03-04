package no.nav.paw.arbeidssokerregisteret.services.kafkakeys

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.jackson
import no.nav.paw.arbeidssokerregisteret.config.KafkaKeysConfig
import no.nav.paw.migrering.app.kafkakeys.KafkaKeysClient
import no.nav.paw.migrering.app.kafkakeys.StandardKafkaKeysClient
import no.nav.paw.migrering.app.kafkakeys.inMemoryKafkaKeysMock


fun kafkaKeysKlient(konfigurasjon: KafkaKeysConfig, m2mTokenFactory: () -> String): KafkaKeysClient =
    when (konfigurasjon.url) {
        "MOCK" -> inMemoryKafkaKeysMock()
        else -> kafkaKeysMedHttpClient(konfigurasjon, m2mTokenFactory)
    }

private fun kafkaKeysMedHttpClient(config: KafkaKeysConfig, m2mTokenFactory: () -> String): KafkaKeysClient {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }
    return StandardKafkaKeysClient(
        httpClient,
        config.url
    ) { m2mTokenFactory() }
}

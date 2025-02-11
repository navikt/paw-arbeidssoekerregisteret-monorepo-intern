package no.nav.paw.client.factory

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.paw.serialization.jackson.configureJackson

fun createHttpClient(
    engineFactory: HttpClientEngineFactory<HttpClientEngineConfig> = CIO,
    block: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                configureJackson()
            }
        }
    }
): HttpClient {
    return HttpClient(engineFactory, block)
}

fun createHttpClient(
    engine: HttpClientEngine,
    block: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                configureJackson()
            }
        }
    }
): HttpClient {
    return HttpClient(engine, block)
}

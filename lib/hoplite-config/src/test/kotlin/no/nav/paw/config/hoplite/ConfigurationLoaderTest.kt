package no.nav.paw.config.hoplite

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

// Miljøvariablene settes i build.gradle.kts: PORT=8080, HOSTNAME=nais-pod,
// TEST_DB_PORT=5432, TEST_DB_HOST=db.example
class ConfigurationLoaderTest : FreeSpec({
    "Miljøvariabler skal bare brukes når de er referert eksplisitt med \${VAR}" - {
        val config = loadConfigFromProvidedResource<TestConfig>("/test/database_config.toml")

        "nestet port med \${TEST_DB_PORT} får verdien fra TEST_DB_PORT, ikke PORT" {
            config.database.port shouldBe 5432
        }
        "toppnivå-port uten \${} beholder verdien fra fila selv om PORT er satt" {
            config.port shouldBe 9090
        }
        "hostname uten \${} beholder verdien fra fila selv om HOSTNAME er satt" {
            config.hostname shouldBe "fra-fil"
        }
        "vanlig \${VAR}-substitusjon virker" {
            config.database.host shouldBe "db.example"
        }
    }

    "strict feiler på ukjente nøkler" {
        shouldThrowAny {
            loadConfigFromProvidedResource<TestConfig>("/test/ukjent_nokkel.toml")
        }
    }
})

data class TestConfig(
    val port: Int,
    val hostname: String,
    val database: DatabaseTestConfig,
)

data class DatabaseTestConfig(
    val host: String,
    val port: Int,
)

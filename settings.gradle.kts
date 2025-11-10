rootProject.name = "paw-arbeidssoekerregisteret-monorepo-intern"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.serialization") version "2.2.21" apply false
    id("com.google.cloud.tools.jib") version "3.4.5" apply false
    id("org.openapi.generator") version "7.17.0" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
    id("com.expediagroup.graphql") version "8.8.1" apply false
}

include(
    // domain
    "domain:felles",
    "domain:error",
    "domain:identitet-interne-hendelser",
    "domain:bekreftelse-interne-hendelser",
    "domain:bekreftelse-paavegneav-avro-schema",
    "domain:bekreftelsesmelding-avro-schema",
    "domain:main-avro-schema",
    "domain:interne-hendelser",
    "domain:arbeidssoekerregisteret-kotlin",
    "domain:arbeidssoeker-regler",
    "domain:pdl-aktoer-schema",
    // libs
    "lib:hoplite-config",
    "lib:tracing",
    "lib:metrics",
    "lib:logging",
    "lib:serialization",
    "lib:error-handling",
    "lib:health",
    "lib:api-docs",
    "lib:security",
    "lib:database",
    "lib:async",
    "lib:scheduling",
    "lib:http-client-utils",
    "lib:kafka",
    "lib:kafka-hwm",
    "lib:kafka-streams",
    "lib:kafka-key-generator-client",
    "lib:api-oppslag-client",
    "lib:pdl-client",
    "lib:aareg-client",
    "lib:tilgangskontroll-client",
    // test
    "test:test-data-lib",
    "test:kafka-streams-test-functions",
    // jobs
    "jobs:arbeidssoekere-synk-jobb",
    // apps
    "apps:api-start-stopp-perioder",
    "apps:hendelseprosessor",
    "apps:hendelselogg-backup",
    "apps:utgang-pdl",
    "apps:kafka-key-generator",
    "apps:bekreftelse-tjeneste",
    "apps:bekreftelse-api",
    "apps:bekreftelse-utgang",
    "apps:bekreftelse-hendelsefilter",
    "apps:bekreftelse-backup",
    "apps:tilgangskontroll",
    "apps:dolly-api",
    "apps:min-side-varsler",
    "apps:bigquery-stats-adapter",
    "apps:dev-proxy",
)

dependencyResolutionManagement {
    val githubPassword: String by settings
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
        maven {
            setUrl("https://maven.pkg.github.com/navikt/poao-tilgang")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven {
            setUrl("https://maven.pkg.github.com/navikt/paw-arbeidssokerregisteret-api")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven {
            setUrl("https://maven.pkg.github.com/navikt/paw-kotlin-clients")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
    }
}

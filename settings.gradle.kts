plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    kotlin("jvm") version "2.0.20" apply false
    id("com.google.cloud.tools.jib") version "3.4.3" apply false
    id("org.openapi.generator") version "7.8.0" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
}

rootProject.name = "paw-arbeidssoekerregisteret-monorepo-intern"

include(
    "lib:hoplite-config",
    "lib:error-handling",
    "lib:kafka",
    "lib:kafka-streams",
    "lib:kafka-key-generator-client",
    "test:test-data-lib",
    "domain:bekreftelse-interne-hendelser",
    "domain:bekreftelsesansvar-avro-schema",
    "domain:bekreftelsesmelding-avro-schema",
    "domain:main-avro-schema",
    "domain:interne-hendelser",
    "domain:arbeidssoekerregisteret-kotlin",
    "domain:arbeidssoeker-regler",
    "apps:api-start-stopp-perioder",
    "apps:hendelseprosessor",
    "apps:utgang-formidlingsgruppe",
    "apps:hendelselogg-backup",
    "apps:utgang-pdl",
    "apps:kafka-key-generator",
    "apps:bekreftelse-tjeneste",
    "apps:bekreftelse-api",
    "apps:bekreftelse-min-side-oppgaver",
    "apps:bekreftelse-utgang",
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    kotlin("jvm") version "2.0.0" apply false
    id("com.google.cloud.tools.jib") version "3.4.3" apply false
    id("org.openapi.generator") version "7.5.0" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
}

rootProject.name = "paw-arbeidssoekerregisteret-monorepo-intern"

include(
    "lib:hoplite-config",
    "lib:error-handling",
    "lib:kafka",
    "lib:kafka-streams",
    "lib:kafka-key-generator-client",
    "domain:bekreftelse-interne-hendelser",
    "domain:bekreftelsesansvar-schema",
    "domain:bekreftelsesmelding-schema",
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
    versionCatalogs {
        // PAW greier
        val pawPdlClientVersion = "24.08.08.40-1"
        val pawAaregClientVersion = "24.07.04.18-1"
        val arbeidssokerregisteretVersion = "1.9348086045.48-1"
        val rapporteringsSchemaVersion = "24.09.11.8-1"


        //Arrow
        val arrowVersion = "1.2.4"

        // NAV
        val noNavCommonVersion = "3.2024.05.23_05.46-2b29fa343e8e" //https://github.com/navikt/common-java-modules
        val noNavSecurityVersion = "5.0.1" //https://github.com/navikt/token-support

        // Konfigurasjon
        val comSksamuelHopliteVersion = "2.8.0.RC3"

        // Kafka
        val orgApacheKafkaVersion = "3.7.0"
        val ioConfluentKafkaVersion = "7.6.0"

        // Serialisering
        val orgApacheAvroVersion = "1.11.3"
        val comFasterxmlJacksonVersion = "2.17.1"

        // ---- //
        val kotlinExposedVersion = "0.51.1"
        val logstashVersion = "7.4"
        val logbackVersion = "1.5.6"
        val kotestVersion = "5.9.1"
        val mockkVersion = "1.13.11"
        val testContainersVersion = "1.19.8"
        val mockOauth2ServerVersion = "2.1.5"
        val micrometerVersion = "1.13.1"
        val otelTargetSdkVersion = "1.39.0"
        val otelInstrumentationVersion = "2.4.0"
        val otelInstrumentationKtorVersion = "2.4.0-alpha"
        val coroutinesVersion = "1.8.1"
        val postgresDriverVersion = "42.7.3"
        val flywayVersion = "10.15.0"
        val hikariVersion = "5.1.0"

        fun VersionCatalogBuilder.ktorLib(alias: String, artifactId: String) =
            library(alias, "io.ktor", artifactId).version("2.3.12")

        fun VersionCatalogBuilder.ktorLibs(vararg aliases: Pair<String, String>) =
            aliases.forEach { (artifactId, alias) -> ktorLib(alias, artifactId) }

        infix fun String.alias(alias: String) = this to alias

        create("kotlinx") {
            library("coroutinesCore", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version(coroutinesVersion)
        }
        create("arrow") {
            library("core", "io.arrow-kt", "arrow-core").version(arrowVersion)
        }
        create("loggingLibs") {
            library("logbackClassic", "ch.qos.logback", "logback-classic").version(logbackVersion)
            library("logstashLogbackEncoder", "net.logstash.logback", "logstash-logback-encoder").version(
                logstashVersion
            )
        }
        create("ktorClient") {
            ktorLibs(
                "ktor-client-content-negotiation" alias "contentNegotiation",
                "ktor-client-core" alias "core",
                "ktor-client-cio" alias "cio",
                "ktor-client-mock" alias "mock",
                "ktor-client-logging" alias "logging",
                "ktor-client-okhttp" alias "okhttp"
            )
        }
        create("ktorServer") {
            bundle(
                "withNettyAndMicrometer", listOf(
                    "core",
                    "coreJvm",
                    "netty",
                    "micrometer"
                )
            )
            ktorLibs(
                "ktor-server-cors" alias "cors",
                "ktor-server-swagger" alias "swagger",
                "ktor-server-call-id" alias "callId",
                "ktor-server-status-pages" alias "statusPages",
                "ktor-server-content-negotiation" alias "contentNegotiation",
                "ktor-server-core-jvm" alias "coreJvm",
                "ktor-server-core" alias "core",
                "ktor-server-openapi" alias "openapi",
                "ktor-server-netty" alias "netty",
                "ktor-server-auth" alias "auth",
                "ktor-server-metrics-micrometer" alias "micrometer",
                "ktor-server-tests-jvm" alias "testJvm"
            )
        }
        create("ktor") {
            ktorLibs(
                "ktor-serialization" alias "serialization",
                "ktor-serialization-jvm" alias "serializationJvm",
                "ktor-serialization-jackson" alias "serializationJackson",
                "ktor-serialization-kotlinx-json" alias "serializationJson"
            )
        }
        create("otel") {
            library("api", "io.opentelemetry", "opentelemetry-api").version(otelTargetSdkVersion)
            library("ktor", "io.opentelemetry.instrumentation", "opentelemetry-ktor-2.0").version(
                otelInstrumentationKtorVersion
            )
            library(
                "annotations",
                "io.opentelemetry.instrumentation",
                "opentelemetry-instrumentation-annotations"
            ).version(otelInstrumentationVersion)
        }
        create("micrometer") {
            library("core", "io.micrometer", "micrometer-core").version(micrometerVersion)
            library("registryPrometheus", "io.micrometer", "micrometer-registry-prometheus").version(
                micrometerVersion
            )
        }
        create("pawClients") {
            library("pawPdlClient", "no.nav.paw", "pdl-client").version(pawPdlClientVersion)
            library("pawAaregClient", "no.nav.paw", "aareg-client").version(pawAaregClientVersion)
        }
        create("arbeidssoekerRegisteret") {
            library("mainAvroSchema", "no.nav.paw.arbeidssokerregisteret.api", "main-avro-schema").version(
                arbeidssokerregisteretVersion
            )
            version("arbeidssokerregisteretVersion", arbeidssokerregisteretVersion)
        }
        create("orgApacheKafka") {
            library("kafkaClients", "org.apache.kafka", "kafka-clients").version(orgApacheKafkaVersion)
            library("kafkaStreams", "org.apache.kafka", "kafka-streams").version(orgApacheKafkaVersion)
            library("streamsTest", "org.apache.kafka", "kafka-streams-test-utils").version(orgApacheKafkaVersion)
        }
        create("apacheAvro") {
            library("avro", "org.apache.avro", "avro").version(orgApacheAvroVersion)
            library("kafkaSerializer", "io.confluent", "kafka-avro-serializer").version(ioConfluentKafkaVersion)
            library("kafkaStreamsAvroSerde", "io.confluent", "kafka-streams-avro-serde").version(
                ioConfluentKafkaVersion
            )
        }
        create("jackson") {
            library("datatypeJsr310", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310").version(
                comFasterxmlJacksonVersion
            )
            library("kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").version(
                comFasterxmlJacksonVersion
            )
            library("core", "com.fasterxml.jackson.core", "jackson-core").version(comFasterxmlJacksonVersion)
        }
        create("navCommon") {
            library("tokenClient", "no.nav.common", "token-client").version(noNavCommonVersion)
            library("log", "no.nav.common", "log").version(noNavCommonVersion)
            library("auditLog", "no.nav.common", "audit-log").version(noNavCommonVersion)
        }
        create("navSecurity") {
            library("tokenValidationKtorV2", "no.nav.security", "token-validation-ktor-v2").version(
                noNavSecurityVersion
            )
            library("tokenClient", "no.nav.security", "token-client-core").version(noNavSecurityVersion)
        }
        create("hoplite") {
            library("hopliteCore", "com.sksamuel.hoplite", "hoplite-core").version(comSksamuelHopliteVersion)
            library("hopliteToml", "com.sksamuel.hoplite", "hoplite-toml").version(comSksamuelHopliteVersion)
            library("hopliteYaml", "com.sksamuel.hoplite", "hoplite-yaml").version(comSksamuelHopliteVersion)
        }
        create("exposed") {
            library("core", "org.jetbrains.exposed", "exposed-core").version(kotlinExposedVersion)
            library("crypt", "org.jetbrains.exposed", "exposed-crypt").version(kotlinExposedVersion)
            library("dao", "org.jetbrains.exposed", "exposed-dao").version(kotlinExposedVersion)
            library("jdbc", "org.jetbrains.exposed", "exposed-jdbc").version(kotlinExposedVersion)
            library("javaTime", "org.jetbrains.exposed", "exposed-java-time").version(kotlinExposedVersion)
        }
        create("testLibs") {
            bundle(
                "withUnitTesting", listOf(
                    "runnerJunit5",
                    "assertionsCore",
                    "mockk"
                )
            )
            library("runnerJunit5", "io.kotest", "kotest-runner-junit5").version(kotestVersion)
            library("assertionsCore", "io.kotest", "kotest-assertions-core").version(kotestVersion)
            library("mockk", "io.mockk", "mockk").version(mockkVersion)
            library("testContainers", "org.testcontainers", "testcontainers").version(testContainersVersion)
            library("postgresql", "org.testcontainers", "postgresql").version(testContainersVersion)
            library("mockOauth2Server", "no.nav.security", "mock-oauth2-server").version(mockOauth2ServerVersion)
        }
        create("postgres") {
            library("driver", "org.postgresql", "postgresql").version(postgresDriverVersion)
        }
        create("flyway") {
            library("core", "org.flywaydb", "flyway-core").version(flywayVersion)
            library("postgres", "org.flywaydb", "flyway-database-postgresql").version(flywayVersion)
        }
        create("hikari") {
            library("connectionPool", "com.zaxxer", "HikariCP").version(hikariVersion)
        }
        create("poao") {
            library("tilgangClient", "no.nav.poao-tilgang", "client").version("2024.04.29_13.59-a0ddddd36ac9")
        }
        create("rapportering") {
            library(
                "rapporteringsansvarSchema",
                "no.nav.paw.arbeidssokerregisteret.api",
                "rapporteringsansvar-schema"
            ).version(rapporteringsSchemaVersion)
            library(
                "rapporteringsmeldingSchema",
                "no.nav.paw.arbeidssokerregisteret.api",
                "rapporteringsmelding-schema"
            ).version(rapporteringsSchemaVersion)
        }
    }
}

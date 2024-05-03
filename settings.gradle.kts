plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
    kotlin("jvm") version "1.9.22" apply false
    id("io.ktor.plugin") version "2.3.10" apply false //hold i synk med "val ktorVersion" lenger nede i filen
    id("com.google.cloud.tools.jib") version "3.4.1" apply false
    id("org.openapi.generator") version "7.4.0" apply false
}

rootProject.name = "paw-arbeidssoekerregisteret-monorepo-intern"

include("api-start-stopp-perioder")

dependencyResolutionManagement {
    val githubPassword: String by settings
    repositories {
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
    }
    versionCatalogs {
        val jvmMajorVersion = 21
        // PAW greier
        val pawPdlClientVersion = "24.03.20.30-1"
        val pawAaregClientVersion = "24.01.12.16-1"
        val arbeidssokerregisteretVersion = "1.8062260419.22-1"

        // NAV
        val noNavCommonVersion = "3.2024.02.21_11.18-8f9b43befae1"
        val noNavSecurityVersion = "3.1.5"

        // Konfigurasjon
        val comSksamuelHopliteVersion = "2.8.0.RC3"

        // Kafkawhic
        val orgApacheKafkaVersion = "3.6.0"
        val ioConfluentKafkaVersion = "7.6.0"

        // Serialisering
        val orgApacheAvroVersion = "1.11.3"
        val comFasterxmlJacksonVersion = "2.16.1"

        // ---- //
        val kotlinExposedVersion = "0.50.0"
        val ktorVersion = "2.3.9"
        val logstashVersion = "7.3"
        val logbackVersion = "1.4.14"
        val kotestVersion = "5.8.1"
        val mockkVersion = "1.13.10"
        val testContainersVersion = "1.19.6"
        val mockOauth2ServerVersion = "2.0.0"
        val micrometerVersion = "1.12.3"

        create("loggingLibs") {
            library(
                "logbackClassic",
                "ch.qos.logback",
                "logback-classic"
            ).version(logbackVersion)
            library(
                "logstashLogbackEncoder",
                "net.logstash.logback",
                "logstash-logback-encoder"
            ).version(logstashVersion)
        }
        create("ktorClient") {
            library(
                "contentNegotiation",
                "io.ktor",
                "ktor-client-content-negotiation"
            ).version(ktorVersion)
            library(
                "core",
                "io.ktor",
                "ktor-client-core"
            ).version(ktorVersion)
            library(
                "cio",
                "io.ktor",
                "ktor-client-cio"
            ).version(ktorVersion)
        }
        create("ktorServer") {
            library(
                "cors",
                "io.ktor",
                "ktor-server-cors"
            ).version(ktorVersion)
            library(
                "swagger",
                "io.ktor",
                "ktor-server-swagger"
            ).version(ktorVersion)
            library(
                "callId",
                "io.ktor",
                "ktor-server-call-id"
            ).version(ktorVersion)
            library(
                "statusPages",
                "io.ktor",
                "ktor-server-status-pages"
            ).version(ktorVersion)
            library(
                "contentNegotiation",
                "io.ktor",
                "ktor-server-content-negotiation"
            ).version(ktorVersion)
            library(
                "coreJvm",
                "io.ktor",
                "ktor-server-core-jvm"
            ).version(ktorVersion)
            library(
                "core",
                "io.ktor",
                "ktor-server-core"
            ).version(ktorVersion)
            library(
                "openapi",
                "io.ktor",
                "ktor-server-openapi"
            ).version(ktorVersion)
            library(
                "netty",
                "io.ktor",
                "ktor-server-netty"
            ).version(ktorVersion)
            library(
                "auth",
                "io.ktor",
                "ktor-server-auth"
            ).version(ktorVersion)
            library(
                "micrometer",
                "io.ktor",
                "ktor-server-metrics-micrometer"
            ).version(ktorVersion)

        }
        create("micrometer") {
            library(
                "core",
                "io.micrometer",
                "micrometer-core"
            ).version(micrometerVersion)
            library(
                "registryPrometheus",
                "io.micrometer",
                "micrometer-registry-prometheus"
            ).version(micrometerVersion)
        }
        create("pawClients") {
            library(
                "pawPdlClient",
                "no.nav.paw",
                "pdl-client"
            ).version(pawPdlClientVersion)
            library(
                "pawAaregClient",
                "no.nav.paw",
                "aareg-client"
            ).version(pawAaregClientVersion)
        }
        create("arbeidssoekerRegisteret") {
            library(
                "apiKotlin",
                "no.nav.paw.arbeidssokerregisteret.api.schema",
                "arbeidssoekerregisteret-kotlin"
            ).version(arbeidssokerregisteretVersion)
            library(
                "mainAvroSchema",
                "no.nav.paw.arbeidssokerregisteret.api",
                "main-avro-schema"
            ).version(arbeidssokerregisteretVersion)
        }
        create("orgApacheKafka") {
            library(
                "kafkaClients",
                "org.apache.kafka",
                "kafka-clients"
            ).version(orgApacheKafkaVersion)
            library(
                "kafkaStreams",
                "org.apache.kafka",
                "kafka-streams"
            ).version(orgApacheKafkaVersion)
            library(
                "streamsTest",
                "org.apache.kafka",
                "kafka-streams-test-utils"
            ).version(orgApacheKafkaVersion)
        }
        create("apacheAvro") {
            library(
                "avro",
                "org.apache.avro",
                "avro"
            ).version(orgApacheAvroVersion)
            library(
                "kafkaSerializer",
                "io.confluent",
                "kafka-avro-serializer"
            ).version(ioConfluentKafkaVersion)
            library(
                "kafkaStreamsAvroSerde",
                "io.confluent",
                "kafka-streams-avro-serde"
            ).version(ioConfluentKafkaVersion)
        }
        create("jackson") {
            library(
                "datatypeJsr310",
                "com.fasterxml.jackson.datatype",
                "jackson-datatype-jsr310"
            ).version(comFasterxmlJacksonVersion)
            library(
                "kotlin",
                "com.fasterxml.jackson.module",
                "jackson-module-kotlin"
            ).version(comFasterxmlJacksonVersion)
            library(
                "ktorSerialization",
                "io.ktor",
                "ktor-serialization-jackson"
            ).version(ktorVersion)
            library(
                "serializationJvm",
                "io.ktor",
                "ktor-serialization-jackson-jvm"
            ).version(ktorVersion)
        }
        create("navCommon") {
            library(
                "tokenClient",
                "no.nav.common",
                "token-client"
            ).version(noNavCommonVersion)
            library(
                "log",
                "no.nav.common",
                "log"
            ).version(noNavCommonVersion)
        }
        create("navSecurity") {
            library(
                "tokenValidationKtorV2",
                "no.nav.security",
                "token-validation-ktor-v2"
            ).version(noNavSecurityVersion)
            library(
                "tokenClient",
                "no.nav.security",
                "token-client-core"
            ).version(noNavSecurityVersion)
        }
        create("hoplite") {
            library(
                "hopliteCore",
                "com.sksamuel.hoplite",
                "hoplite-core"
            ).version(comSksamuelHopliteVersion)
            library(
                "hopliteToml",
                "com.sksamuel.hoplite",
                "hoplite-toml"
            ).version(comSksamuelHopliteVersion)
            library(
                "hopliteYaml",
                "com.sksamuel.hoplite",
                "hoplite-yaml"
            ).version(comSksamuelHopliteVersion)
        }
        create("exposed") {
            library(
                "core",
                "org.jetbrains.exposed",
                "exposed-core"
            ).version(kotlinExposedVersion)
            library(
                "crypt",
                "org.jetbrains.exposed",
                "exposed-crypt"
            ).version(kotlinExposedVersion)
            library(
                "dao",
                "org.jetbrains.exposed",
                "exposed-dao"
            ).version(kotlinExposedVersion)
            library(
                "jdbc",
                "org.jetbrains.exposed",
                "exposed-jdbc"
            ).version(kotlinExposedVersion)
            library(
                "javaTime",
                "org.jetbrains.exposed",
                "exposed-java-time"
            ).version(kotlinExposedVersion)
        }
        create("testLibs") {
            library(
                "runnerJunit5",
                "io.kotest",
                "kotest-runner-junit5"
            ).version(kotestVersion)
            library(
                "assertionsCore",
                "io.kotest",
                "kotest-assertions-core"
            ).version(kotestVersion)
            library(
                "mockk",
                "io.mockk",
                "mockk"
            ).version(mockkVersion)
            library(
                "testContainers",
                "org.testcontainers",
                "testcontainers"
            ).version(testContainersVersion)
            library(
                "postgresql",
                "org.testcontainers",
                "postgresql"
            ).version(testContainersVersion)
            library(
                "mockOauth2Server",
                "no.nav.security",
                "mock-oauth2-server"
            ).version(mockOauth2ServerVersion)
        }
    }
}
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
//    id("org.jmailen.kotlinter") version "3.16.0"
    application
}

val logbackVersion = "1.4.5"
val logstashVersion = "7.3"
val navCommonModulesVersion = "2.2023.01.02_13.51-1c6adeb1653b"
val tokenSupportVersion = "3.1.5"
val koTestVersion = "5.7.2"
val hopliteVersion = "2.8.0.RC3"
val ktorVersion = pawObservability.versions.ktor
val arbeidssokerregisteretVersion = "23.12.18.110-1"
val pawUtilsVersion = "23.12.20.5-1"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    mavenNav("paw-arbeidssokerregisteret")
}

dependencies {
    implementation("no.nav.paw.arbeidssokerregisteret.internt.schema:interne-eventer:$arbeidssokerregisteretVersion")
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.common:token-client:$navCommonModulesVersion")
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("io.confluent:kafka-avro-serializer:7.4.0")
    implementation("com.github.navikt.poao-tilgang:client:2023.09.25_09.26-72043f243cad")
    implementation("no.nav.paw:pdl-client:0.3.7")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")

    // TODO: Flytte til bundle KTOR
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$koTestVersion")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.testcontainers:testcontainers:1.19.1")
    testImplementation("no.nav.security:mock-oauth2-server:2.0.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.ApplicationKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

fun RepositoryHandler.mavenNav(repo: String): MavenArtifactRepository {
    val githubPassword: String by project

    return maven {
        setUrl("https://maven.pkg.github.com/navikt/$repo")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}

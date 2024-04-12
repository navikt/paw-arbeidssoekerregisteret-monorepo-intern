import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("io.ktor.plugin") version "2.3.8"
    id("com.google.cloud.tools.jib") version "3.4.1"
    application
}
val jvmVersion = JavaVersion.VERSION_21
val image: String? by project

val logbackVersion = "1.4.14"
val logstashVersion = "7.3"

val arbeidssokerregisteretSchemaVersion = "1.8657441125.42-1"

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:$arbeidssokerregisteretSchemaVersion")
    implementation("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:$arbeidssokerregisteretSchemaVersion")
    implementation(project(":interne-eventer"))
    implementation(project(":arbeidssoekerregisteret-kotlin"))
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
    implementation("com.sksamuel.hoplite:hoplite-core:2.8.0.RC3")
    implementation("com.sksamuel.hoplite:hoplite-toml:2.8.0.RC3")
    implementation("no.nav.common:log:3.2024.02.21_11.18-8f9b43befae1")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    implementation("org.apache.kafka:kafka-streams:3.7.0")
    implementation("io.confluent:kafka-avro-serializer:7.6.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.6.0")
    implementation("org.apache.avro:avro:1.11.3")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.0")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.7.0")
    implementation("io.prometheus:client_java:1.1.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.0")
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    schema.forEach {
        source(zipTree(it))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class) {

}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.app.AppKt")
}

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

jib {
    from.image = "ghcr.io/navikt/baseimages/temurin:${jvmVersion.majorVersion}"
    to.image = "${image ?: rootProject.name }:${project.version}"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

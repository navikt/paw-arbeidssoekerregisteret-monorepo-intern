import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("io.ktor.plugin") version "2.3.5"
    application
}
val logbackVersion = "1.4.5"
val logstashVersion = "7.3"

dependencies {
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
    implementation("com.sksamuel.hoplite:hoplite-core:2.8.0.RC3")
    implementation("com.sksamuel.hoplite:hoplite-toml:2.8.0.RC3")
    implementation("no.nav.common:log:2.2023.01.10_13.49-81ddc732df3a")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("org.apache.kafka:kafka-streams:3.5.1")
    implementation("io.confluent:kafka-avro-serializer:7.4.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.4.0")
    implementation("org.apache.avro:avro:1.11.0")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.0")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.5.1")
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class) {
    source("$rootDir/eksternt-api/src/main")
    source("$rootDir/interne-eventer/src/main")
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.app.AppKt")
}

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

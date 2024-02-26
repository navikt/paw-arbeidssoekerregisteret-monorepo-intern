import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("io.ktor.plugin") version "2.3.8"
    id("com.google.cloud.tools.jib") version "3.4.0"
    application
}
val jvmVersion = JavaVersion.VERSION_21
val image: String? by project

val logbackVersion = "1.4.5"
val logstashVersion = "7.3"

val agent by configurations.creating {
    isTransitive = false
}

val agentExtension by configurations.creating {
    isTransitive = false
}

val agentExtensionJar = "agent-extension.jar"
val agentJar = "agent.jar"
val agentFolder = layout.buildDirectory.dir("agent").get().toString()
val agentExtensionFolder = layout.buildDirectory.dir("agent-extension").get().toString()

val arbeidssokerregisteretSchemaVersion = "1.8050675667.20-1"

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:$arbeidssokerregisteretSchemaVersion")
    agent("io.opentelemetry.javaagent:opentelemetry-javaagent:${pawObservability.versions.openTelemetryInstrumentation.get()}")
    agentExtension("no.nav.paw.observability:opentelemetry-anonymisering-${pawObservability.versions.openTelemetryInstrumentation.get()}:24.02.20.10-1")
    implementation(project(":interne-eventer"))
    implementation(project(":arbeidssoekerregisteret-kotlin"))
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

tasks.create("addAgent", Copy::class) {
    from(agent)
    into(agentFolder)
    rename { _ -> agentJar}
}

tasks.create("addAgentExtension", Copy::class) {
    from(agentExtension)
    into(agentExtensionFolder)
    rename { _ -> agentExtensionJar}
}

tasks.withType(KotlinCompile::class) {
    dependsOn.add("addAgent")
    dependsOn.add("addAgentExtension")
}

jib {
    from.image = "ghcr.io/navikt/baseimages/temurin:${jvmVersion.majorVersion}"
    to.image = "${image ?: rootProject.name }:${project.version}"
    extraDirectories {
        paths {
            path {
                setFrom(agentFolder)
                into = "/app"
            }
            path {
                setFrom(agentExtensionFolder)
                into = "/app"
            }
        }
    }
    container.entrypoint = listOf(
        "java",
        "-cp", "@/app/jib-classpath-file",
        "-javaagent:/app/$agentJar",
        "-Dotel.javaagent.extensions=/app/$agentExtensionJar",
        "-Dotel.resource.attributes=service.name=${project.name}",
        application.mainClass.get()
    )
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

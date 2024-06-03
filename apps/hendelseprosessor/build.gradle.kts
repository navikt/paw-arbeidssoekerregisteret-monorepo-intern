import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro")
    id("com.google.cloud.tools.jib")
    application
}
val jvmVersion = JavaVersion.VERSION_21
val image: String? by project

dependencies {
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:arbeidssoekerregisteret-kotlin"))
    implementation(project(":domain:main-avro-schema"))

    implementation(otel.annotations)
    implementation(jackson.datatypeJsr310)
    implementation(jackson.kotlin)
    implementation(project(":lib:hoplite-config"))
    implementation(navCommon.log)
    implementation(orgApacheKafka.kafkaClients)
    implementation(orgApacheKafka.kafkaStreams)
    implementation(apacheAvro.avro)
    implementation(apacheAvro.kafkaStreamsAvroSerde)

    implementation(micrometer.registryPrometheus)
    implementation(ktorServer.core)
    implementation(ktorServer.micrometer)
    implementation(ktorServer.netty)
    implementation(ktorServer.coreJvm)

    testImplementation(testLibs.runnerJunit5)
    testImplementation(orgApacheKafka.streamsTest)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
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

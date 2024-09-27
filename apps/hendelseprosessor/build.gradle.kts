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
    implementation(project(":lib:hoplite-config"))

    implementation(libs.opentelemetryAnnotations)
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.jacksonKotlin)
    implementation(libs.log)
    implementation(libs.kafkaClients)
    implementation(libs.kafkaStreams)
    implementation(libs.avro)
    implementation(libs.kafkaStreamsAvroSerde)

    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.ktorServerCore)
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerCoreJvm)

    testImplementation(libs.runnerJunit5)
    testImplementation(libs.streamsTest)
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

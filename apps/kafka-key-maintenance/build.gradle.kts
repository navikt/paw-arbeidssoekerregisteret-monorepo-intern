import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("jib-chainguard")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:pdl-aktoer-schema"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:tracing"))
    implementation(kotlin("reflect"))

    implementation(libs.arrow.core.core)
    implementation(libs.arrow.functions)
    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.callId)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.jvm)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.nav.security.tokenValidationKtorV3)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.log)

    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.annotations)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.toml)
    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.log)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams.core)
    implementation(libs.avro.core)
    implementation(libs.avro.kafkaSerializer)
    implementation(libs.avro.kafkaStreamsSerde)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.database.postgres.driver)
    implementation(libs.database.flyway.core)
    implementation(libs.database.flyway.postgres)
    implementation(libs.opentelemetry.api)

    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.jackson.kotlin)

    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
    testImplementation(libs.test.mockk.core)
    testImplementation(libs.test.testContainers.core)
    testImplementation(libs.test.testContainers.postgresql)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kafka.streams.test)
    testImplementation(project(":test:kafka-streams-test-functions"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.kafkakeymaintenance.AppStartupKt")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

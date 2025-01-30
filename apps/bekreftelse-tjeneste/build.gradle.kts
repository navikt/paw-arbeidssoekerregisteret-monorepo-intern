
plugins {
    kotlin("jvm")
    id("jib-distroless")
    application
}

val jvmMajorVersion: String by project

dependencies {
    // Project
    implementation(project(":lib:common-model"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:bekreftelse-paavegneav-avro-schema"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))

    // Server
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)

    // Serialization
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.jackson.datatypeJsr310)

    // Tooling
    implementation(libs.arrow.core.core)
    implementation(libs.arrow.functions)
    implementation(libs.arrow.integration.jackson)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.auditLog)

    // Instrumentation
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.annotations)

    // Kafka
    implementation(libs.kafka.streams.core)
    implementation(libs.avro.kafkaStreamsSerde)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kafka.streams.test)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(project(":test:test-data-lib"))
    testImplementation(project(":test:kafka-streams-test-functions"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.bekreftelsetjeneste.ApplicationKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

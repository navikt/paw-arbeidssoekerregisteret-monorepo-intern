plugins {
    kotlin("jvm")
    id("jib-distroless")
}

val jvmMajorVersion: String by project

dependencies {
    // Project
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:logging"))
    implementation(project(":lib:serialization"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:metrics"))
    implementation(project(":lib:database"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:main-avro-schema"))

    // Ktor
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)

    // Jackson
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatypeJsr310)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

    // Observability
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    // Database
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.database.hikari.connectionPool)
    implementation(libs.database.postgres.driver)
    implementation(libs.database.flyway.postgres)

    // Kafka
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams.core)
    implementation(libs.avro.kafkaStreamsSerde)

    // Utils
    implementation(libs.arrow.core.core)
    implementation(libs.hoplite.yaml)

    // Nav
    implementation(libs.nav.common.log)
    implementation(libs.nav.tms.varsel.kotlinBuilder)

    // Testing
    testImplementation(project(":test:test-data-lib"))
    testImplementation(project(":lib:kafka-key-generator-client"))
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.kafka.streams.test)
    testImplementation("com.h2database:h2:2.3.232") // TODO: Legg inn i Gradle libs
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}


plugins {
    kotlin("jvm")
    application
    id("jib-distroless")
}

val jvmMajorVersion: String by project

dependencies {
    // PAW
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:pdl-client"))
    implementation(project(":lib:kafka"))
    implementation(project(":domain:interne-hendelser"))

    // NAV
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.security.tokenClientCore)
    implementation(libs.nav.security.tokenValidationKtorV3)

    // Kafka (for å beregne partisjonsnummer)
    implementation(libs.kafka.clients)

    // Ktor
    implementation(libs.ktor.serialization.jackson)

    // Ktor Server
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.callId)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.contentNegotiation)

    // Ktor Client
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)

    // Micrometer & OTEL
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.database.postgres.driver)
    implementation(libs.database.flyway.core)
    implementation(libs.database.flyway.postgres)
    implementation(libs.database.hikari.connectionPool)

    // Config
    implementation(libs.hoplite.toml)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

    // Tester
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.test.testContainers.core)
    testImplementation(libs.test.testContainers.postgresql)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.kafkakeygenerator.ApplicationKt")
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

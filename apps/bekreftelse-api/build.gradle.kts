import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    id("com.google.cloud.tools.jib")
    application
}

val jvmMajorVersion: String by project
val baseImage: String by project
val image: String? by project

dependencies {
    // Project
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))

    // Server
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.callId)
    implementation(libs.ktor.server.auth)

    // Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)

    // Serialization
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.serialization.json)
    implementation(libs.jackson.datatypeJsr310)

    // Authentication
    implementation(libs.nav.security.tokenValidationKtorV2)

    // Authorization
    implementation(libs.poao.tilgangClient)

    // Documentation
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.auditLog)

    // Instrumentation
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    // Database
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.javaTime)
    implementation(libs.database.hikari.connectionPool)
    implementation(libs.database.postgres.driver)
    implementation(libs.database.flyway.postgres)

    // Kafka
    implementation(libs.kafka.clients)
    implementation(libs.avro.kafkaStreamsSerde)

    // Test
    testImplementation(libs.ktor.server.testJvm)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.test.mockOauth2Server)
    testImplementation(libs.test.testContainers.postgresql)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.bekreftelse.api.ApplicationKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

jib {
    from.image = "$baseImage:$jvmMajorVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}"
        )
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
    }
}

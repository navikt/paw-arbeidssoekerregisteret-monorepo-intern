import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("com.google.cloud.tools.jib")
}

val image: String? by project
val baseImage: String by project
val jvmMajorVersion: String by project

dependencies {
    // PAW
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:pdl-client"))

    // NAV
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.security.tokenClientCore)
    implementation(libs.nav.security.tokenValidationKtorV2)

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
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)

    // Micrometer & OTEL
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)
    implementation(libs.opentelemetry.ktor)

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
    testImplementation(libs.ktor.server.testJvm)
    testImplementation(libs.ktor.client.mock)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.kafkakeygenerator.AppStarterKt")
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
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}",
            "OTEL_INSTRUMENTATION_METHODS_INCLUDE" to ("io.ktor.server.routing.Routing[interceptor,executeResult];" +
                    "io.ktor.server.netty.NettyApplicationCallHandler[handleRequest,exceptionCaught];") +
                    "io.ktor.serialization.jackson.JacksonConverter[deserialize,serializeNullable]"
        )
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.create("runTestApp", JavaExec::class) {
    classpath = sourceSets["test"].runtimeClasspath +
            sourceSets["main"].runtimeClasspath
    mainClass = "no.nav.paw.kafkakeygenerator.Run_test_appKt"
    args = listOf()
}

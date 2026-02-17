plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    id("jib-chainguard")
    application
}

val jvmMajorVersion: String by project

dependencies {
    // PAW
    implementation(project(":domain:felles"))
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:identitet-interne-hendelser"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:pdl-aktoer-schema"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:serialization"))
    implementation(project(":lib:logging"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:metrics"))
    implementation(project(":lib:security"))
    implementation(project(":lib:database"))
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:pdl-client"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-hwm"))
    implementation(project(":lib:scheduling"))

    // NAV
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.security.tokenClientCore)
    implementation(libs.nav.security.tokenValidationKtorV3)

    // Kafka
    implementation(libs.kafka.clients)
    implementation(libs.avro.kafkaSerializer)
    implementation(libs.avro.kafkaStreamsSerde)

    // Ktor
    implementation(libs.ktor.serialization.jackson)

    // Ktor Server
    implementation(libs.bundles.ktor.server.instrumented)
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
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    // Database
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.database.postgres.driver)
    implementation(libs.database.flyway.core)
    implementation(libs.database.flyway.postgres)
    implementation(libs.database.hikari.connectionPool)

    // Config
    implementation(libs.hoplite.toml)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)

    // Tester
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.test.testContainers.core)
    testImplementation(libs.test.testContainers.postgresql)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.nav.security.mockOauth2Server)
    testImplementation(libs.atlassian.oai.swaggerRequestValidator.core)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.kafkakeygenerator.ApplicationKt")
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("openApiValidate", "openApiGenerate")
}

tasks.named("compileTestKotlin") {
    dependsOn("openApiValidate", "openApiGenerate")
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

val openApiDocFile = "${layout.projectDirectory}/src/main/resources/openapi/documentation.yaml"

openApiValidate {
    inputSpec = openApiDocFile
}

openApiGenerate {
    generatorName = "kotlin"
    inputSpec = openApiDocFile
    outputDir = "${layout.buildDirectory.get()}/generated/"
    packageName = "no.nav.paw.kafkakeygenerator.api"
    configOptions = mapOf(
        "serializationLibrary" to "jackson",
        "enumPropertyNaming" to "original",
    )
    globalProperties = mapOf(
        "apis" to "none",
        "models" to ""
    )
    typeMappings = mapOf(
        "DateTime" to "Instant"
    )
    importMappings = mapOf(
        "Instant" to "java.time.Instant"
    )
}

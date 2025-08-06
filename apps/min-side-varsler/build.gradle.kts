plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    id("jib-distroless")
    application
}

val jvmMajorVersion: String by project

dependencies {
    // Project
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:logging"))
    implementation(project(":lib:serialization"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:metrics"))
    implementation(project(":lib:api-docs"))
    implementation(project(":lib:security"))
    implementation(project(":lib:database"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:scheduling"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:main-avro-schema"))

    // Ktor
    implementation(libs.bundles.ktor.server.instrumented)

    // Serialization
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatypeJsr310)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

    // Observability
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    // Documentation
    implementation(libs.ktor.server.swagger)

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
    implementation(libs.hoplite.yaml)

    // Nav
    implementation(libs.nav.common.log)
    implementation(libs.nav.tms.varsel.kotlinBuilder)
    implementation(libs.nav.security.tokenValidationKtorV3)

    // Testing
    testImplementation(project(":test:test-data-lib"))
    testImplementation(project(":lib:kafka-key-generator-client"))
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.contentNegotiation)
    testImplementation(libs.kafka.streams.test)
    testImplementation(libs.database.h2)
    testImplementation(libs.test.testContainers.postgresql)
    testImplementation(libs.nav.security.mockOauth2Server)
}

application {
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.ApplicationKt")
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

val openApiDocFile = "${layout.projectDirectory}/src/main/resources/openapi/documentation.yaml"

openApiValidate {
    inputSpec = openApiDocFile
}

openApiGenerate {
    generatorName = "kotlin"
    inputSpec = openApiDocFile
    outputDir = "${layout.buildDirectory.get()}/generated/"
    packageName = "no.nav.paw.arbeidssoekerregisteret.api"
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


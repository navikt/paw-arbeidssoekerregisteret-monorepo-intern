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
    implementation(project(":lib:security"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:tilgangskontroll-client"))
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
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.jackson.datatypeJsr310)

    // Authentication
    implementation(libs.nav.security.tokenValidationKtorV3)

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
    testImplementation(libs.ktor.server.test.host)
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
    packageName = "no.nav.paw.bekreftelse.api"
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

jib {
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}"
        )
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
    }
}

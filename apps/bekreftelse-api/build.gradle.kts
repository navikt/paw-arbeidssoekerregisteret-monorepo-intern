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
    implementation(project(":lib:error-handling-ktor3"))
    implementation(project(":lib:security"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client-ktor3"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))

    // Server
    implementation(libs.bundles.ktor3ServerWithNettyAndMicrometer)
    implementation(libs.ktor3.server.contentNegotiation)
    implementation(libs.ktor3.server.statusPages)
    implementation(libs.ktor3.server.cors)
    implementation(libs.ktor3.server.callId)
    implementation(libs.ktor3.server.auth)

    // Client
    implementation(libs.ktor3.client.core)
    implementation(libs.ktor3.client.cio)
    implementation(libs.ktor3.client.contentNegotiation)

    // Serialization
    implementation(libs.ktor3.serialization.jackson)
    implementation(libs.ktor3.serialization.kotlinx.json)
    implementation(libs.jackson.datatypeJsr310)

    // Authentication
    implementation(libs.nav.security.tokenValidationKtorV3)

    // Authorization
    implementation(libs.nav.poao.tilgangClient)

    // Documentation
    implementation(libs.ktor3.server.openapi)
    implementation(libs.ktor3.server.swagger)

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
    testImplementation(libs.ktor3.server.test.host)
    testImplementation(libs.ktor3.client.mock)
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
    from.image = "$baseImage:$jvmMajorVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}"
        )
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
    }
}

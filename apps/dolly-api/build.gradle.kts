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
    implementation(project(":lib:serialization"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:health"))
    implementation(project(":lib:security"))
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:logging"))
    implementation(project(":lib:metrics"))
    implementation(project(":domain:arbeidssoekerregisteret-kotlin"))
    implementation(project(":domain:interne-hendelser"))

    // Server
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.callId)
    implementation(libs.ktor.server.callLogging)
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

    // Kafka
    implementation(libs.kafka.clients)
    implementation(libs.avro.kafkaStreamsSerde)

    // Test
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.test.mockOauth2Server)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.dolly.api.ApplicationKt")
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
    packageName = "no.nav.paw.dolly.api"
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

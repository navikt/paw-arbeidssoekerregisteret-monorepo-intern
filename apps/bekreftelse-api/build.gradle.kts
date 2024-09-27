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
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorServerStatusPages)
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerCallId)
    implementation(libs.ktorServerAuth)

    // Client
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientContentNegotiation)

    // Serialization
    implementation(libs.ktorSerializationJackson)
    implementation(libs.ktorSerializationJson)
    implementation(libs.jacksonDatatypeJsr310)

    // Authentication
    implementation(libs.tokenValidationKtorV2)

    // Authorization
    implementation(libs.tilgangClient)

    // Documentation
    implementation(libs.ktorServerOpenapi)
    implementation(libs.ktorServerSwagger)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.log)
    implementation(libs.auditLog)

    // Instrumentation
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.opentelemetryApi)
    implementation(libs.opentelemetryAnnotations)

    // Kafka
    implementation(libs.kafkaStreams)
    implementation(libs.kafkaStreamsAvroSerde)

    // Test
    testImplementation(libs.ktorServerTestsJvm)
    testImplementation(libs.ktorClientMock)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.mockOauth2Server)
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
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
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:bekreftelsesansvar-avro-schema"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))

    // Server
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)

    // Serialization
    implementation(libs.ktorSerializationJackson)
    implementation(libs.ktorSerializationJson)
    implementation(libs.jacksonDatatypeJsr310)

    // Tooling
    implementation(libs.arrowCore)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.log)
    implementation(libs.auditLog)

    // Instrumentation
    implementation(libs.micrometerRegistryPrometheus)

    // Kafka
    implementation(libs.kafkaStreams)
    implementation(libs.kafkaStreamsAvroSerde)

    // Testing
    testImplementation(libs.ktorServerTestsJvm)
    testImplementation(libs.streamsTest)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.bekreftelse.tjeneste.StartupKt")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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

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
    implementation(project(":lib:error-handling-ktor3"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client-ktor3"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:interne-hendelser"))

    // Server
    implementation(libs.bundles.ktor3ServerWithNettyAndMicrometer)

    // Serialization
    implementation(libs.ktor3.serialization.jackson)
    implementation(libs.ktor3.serialization.kotlinx.json)
    implementation(libs.jackson.datatypeJsr310)

    // Tooling
    implementation(libs.arrow.core.core)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.auditLog)

    // Instrumentation
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.annotations)

    // Kafka
    implementation(libs.kafka.streams.core)
    implementation(libs.avro.kafkaStreamsSerde)

    // Testing
    testImplementation(libs.ktor3.server.test.host)
    testImplementation(libs.kafka.streams.test)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(project(":test:test-data-lib"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.bekreftelseutgang.ApplicationKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed")
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
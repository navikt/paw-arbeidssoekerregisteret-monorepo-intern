plugins {
    kotlin("jvm")
    id("jib-chainguard")
    id("com.github.davidmc24.gradle.plugin.avro")
    application
}

val jvmMajorVersion: String by project

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:arbeidssoekerregisteret-kotlin"))
    implementation(project(":domain:arbeidssoeker-regler"))

    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:pdl-client"))

    api(libs.arrow.core.core)

    implementation(libs.kafka.streams.core)

    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)

    implementation(libs.nav.common.log)
    implementation(libs.nav.common.tokenClient)

    implementation(libs.avro.kafkaSerializer)
    implementation(libs.avro.kafkaStreamsSerde)
    implementation(libs.avro.core)

    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.serialization.jvm)

    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
    testImplementation(libs.test.testContainers.core)
    testImplementation(libs.nav.security.mockOauth2Server)
    testImplementation(libs.test.mockk.core)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.kafka.streams.test)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmMajorVersion)
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.utgang.pdl.StartupKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Title"] = rootProject.name
        attributes["Main-Class"] = application.mainClass.get()
    }
}
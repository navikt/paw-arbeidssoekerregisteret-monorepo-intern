
plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro")
    id("jib-distroless")
    application
}
val jvmMajorVersion: String by project

dependencies {
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:arbeidssoekerregisteret-kotlin"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":lib:hoplite-config"))

    implementation(libs.opentelemetry.annotations)
    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.nav.common.log)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams.core)
    implementation(libs.avro.core)
    implementation(libs.avro.kafkaStreamsSerde)

    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.ktor.server.core)
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.coreJvm)

    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.kafka.streams.test)
    testImplementation(project(":test:kafka-streams-test-functions"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmMajorVersion)
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.app.AppKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm")
    id("jib-distroless")
    application
}

val jvmMajorVersion: String by project

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:arbeidssoekerregisteret-kotlin"))

    implementation(project(":lib:kafka"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))

    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.coreJvm)
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.micrometer.registryPrometheus)

    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.nav.common.log)

    implementation(libs.kafka.streams.core)

    implementation(libs.avro.kafkaSerializer)
    implementation(libs.avro.kafkaStreamsSerde)
    implementation(libs.avro.core)

    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatypeJsr310)

    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.kafka.streams.test)
    testImplementation(libs.test.kotest.assertionsCore)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmMajorVersion)
    }
}

application {
    mainClass = "no.nav.paw.arbeidssoekerregisteret.app.StartupKt"
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
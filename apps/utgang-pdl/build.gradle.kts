plugins {
    kotlin("jvm")
    id("com.google.cloud.tools.jib")
    id("com.github.davidmc24.gradle.plugin.avro")
    application
}

val baseImage: String by project
val jvmMajorVersion: String by project

val image: String? by project

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
    implementation(project(":lib:kafka-key-generator-client-ktor3"))
    implementation(project(":lib:pdl-client-ktor3"))

    api(libs.arrow.core.core)

    implementation(libs.kafka.streams.core)

    implementation(libs.bundles.ktor3ServerWithNettyAndMicrometer)
    implementation(libs.micrometer.registryPrometheus)

    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

    implementation(libs.nav.common.log)
    implementation(libs.nav.common.tokenClient)

    implementation(libs.avro.kafkaSerializer)
    implementation(libs.avro.kafkaStreamsSerde)
    implementation(libs.avro.core)

    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.ktor3.serialization.jackson)
    implementation(libs.ktor3.serialization.jvm)

    implementation(libs.ktor3.client.contentNegotiation)
    implementation(libs.ktor3.client.core)
    implementation(libs.ktor3.client.cio)

    testImplementation(libs.ktor3.server.test.host)
    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
    testImplementation(libs.test.testContainers.core)
    testImplementation(libs.test.mockOauth2Server)
    testImplementation(libs.test.mockk.core)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
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

jib {
    from.image = "$baseImage:$jvmMajorVersion"
    val actualImage = "${image ?: rootProject.name}:${project.version}"
    to.image = actualImage
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to actualImage
        )
    }
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
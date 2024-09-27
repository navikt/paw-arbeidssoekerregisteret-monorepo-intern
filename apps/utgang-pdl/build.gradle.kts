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

    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:hoplite-config"))

    api(libs.arrowCore)

    implementation(libs.kafkaStreams)
    implementation(libs.pawPdlClient)

    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.micrometerRegistryPrometheus)

    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

    implementation(libs.log)
    implementation(libs.tokenClient)

    implementation(libs.kafkaSerializer)
    implementation(libs.kafkaStreamsAvroSerde)
    implementation(libs.avro)

    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.ktorSerializationJackson)
    implementation(libs.ktorSerializationJvm)

    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)

    testImplementation(libs.ktorServerTestsJvm)
    testImplementation(libs.runnerJunit5)
    testImplementation(libs.assertionsCore)
    testImplementation(libs.testContainers)
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.streamsTest)
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
    from.image = "ghcr.io/navikt/baseimages/temurin:${jvmMajorVersion}"
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
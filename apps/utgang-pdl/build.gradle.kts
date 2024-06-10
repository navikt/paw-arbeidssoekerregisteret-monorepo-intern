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

    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:hoplite-config"))

    implementation(orgApacheKafka.kafkaStreams)

    implementation(ktorServer.bundles.withNettyAndMicrometer)
    implementation(micrometer.registryPrometheus)

    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)

    implementation(navCommon.log)
    implementation(navCommon.tokenClient)

    implementation(apacheAvro.kafkaSerializer)
    implementation(apacheAvro.kafkaStreamsAvroSerde)
    implementation(apacheAvro.avro)

    implementation(pawClients.pawPdlClient)

    implementation(jackson.datatypeJsr310)
    implementation(ktor.serializationJackson)
    implementation(ktor.serializationJvm)

    implementation(ktorClient.contentNegotiation)
    implementation(ktorClient.core)
    implementation(ktorClient.cio)

    testImplementation(ktorServer.testJvm)
    testImplementation(testLibs.runnerJunit5)
    testImplementation(testLibs.assertionsCore)
    testImplementation(testLibs.testContainers)
    testImplementation(testLibs.mockOauth2Server)
    testImplementation(testLibs.mockk)
    testImplementation(orgApacheKafka.streamsTest)
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
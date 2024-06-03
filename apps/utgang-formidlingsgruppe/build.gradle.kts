plugins {
    kotlin("jvm")
    id("com.google.cloud.tools.jib")
    application
}

val baseImage: String by project
val jvmMajorVersion: String by project

val arbeidssokerregisteretVersion = "24.03.25.160-1"

val image: String? by project

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:arbeidssoekerregisteret-kotlin"))

    implementation(ktorServer.netty)
    implementation(ktorServer.core)
    implementation(ktorServer.coreJvm)
    implementation(ktorServer.micrometer)
    implementation(micrometer.registryPrometheus)

    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)
    implementation(navCommon.log)

    implementation(orgApacheKafka.kafkaStreams)
    implementation(project(":lib:kafka"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))

    implementation(apacheAvro.kafkaSerializer)
    implementation(apacheAvro.kafkaStreamsAvroSerde)
    implementation(apacheAvro.avro)

    implementation(jackson.kotlin)
    implementation(jackson.datatypeJsr310)

    testImplementation(testLibs.runnerJunit5)
    testImplementation(orgApacheKafka.streamsTest)
    testImplementation(testLibs.assertionsCore)

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
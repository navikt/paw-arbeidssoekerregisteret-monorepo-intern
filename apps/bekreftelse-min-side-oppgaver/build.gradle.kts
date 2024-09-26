import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.google.cloud.tools.jib")
}

val baseImage: String by project
val jvmMajorVersion: String by project

val image: String? by project

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":lib:error-handling"))
    implementation(project(":domain:main-avro-schema"))
    implementation(jackson.datatypeJsr310)
    implementation(jackson.kotlin)
    implementation(arrow.core)
    implementation(hoplite.hopliteYaml)

    implementation(navCommon.log)

    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)

    implementation(orgApacheKafka.kafkaClients)
    implementation(orgApacheKafka.kafkaStreams)
    implementation(apacheAvro.avro)
    implementation(apacheAvro.kafkaStreamsAvroSerde)

    implementation(ktorServer.bundles.withNettyAndMicrometer)
    implementation(micrometer.registryPrometheus)

    implementation("no.nav.tms.varsel:kotlin-builder:1.1.0")

    testImplementation(orgApacheKafka.streamsTest)
    testImplementation(testLibs.runnerJunit5)
    testImplementation(testLibs.assertionsCore)
    testImplementation(project(":lib:kafka-key-generator-client"))
    testImplementation(project(":test:test-data-lib"))
}

//enable context receiver
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
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}")
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
    }
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":lib:error-handling"))
    implementation(project(":domain:main-avro-schema"))
    implementation(jackson.datatypeJsr310)
    implementation(jackson.kotlin)
    implementation(arrow.core)

    implementation(orgApacheKafka.kafkaClients)
    implementation(orgApacheKafka.kafkaStreams)
    implementation(apacheAvro.avro)
    implementation(apacheAvro.kafkaStreamsAvroSerde)

    implementation(ktorServer.bundles.withNettyAndMicrometer)

    implementation("no.nav.tms.varsel:kotlin-builder:1.0.0")

    testImplementation(orgApacheKafka.streamsTest)
    testImplementation(testLibs.runnerJunit5)
    testImplementation(testLibs.assertionsCore)
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

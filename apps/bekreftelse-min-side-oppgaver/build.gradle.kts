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
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.jacksonKotlin)
    implementation(libs.arrowCore)
    implementation(libs.hopliteYaml)
    implementation(libs.log)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.kafkaClients)
    implementation(libs.kafkaStreams)
    implementation(libs.avro)
    implementation(libs.kafkaStreamsAvroSerde)
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.micrometerRegistryPrometheus)
    implementation("no.nav.tms.varsel:kotlin-builder:1.1.0")
    testImplementation(libs.streamsTest)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(project(":lib:kafka-key-generator-client"))
    testImplementation(project(":test:test-data-lib"))
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

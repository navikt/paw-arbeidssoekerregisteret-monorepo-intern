
plugins {
    kotlin("jvm")
    id("jib-distroless")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":lib:error-handling"))
    implementation(project(":domain:main-avro-schema"))
    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.arrow.core.core)
    implementation(libs.hoplite.yaml)
    implementation(libs.nav.common.log)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams.core)
    implementation(libs.avro.core)
    implementation(libs.avro.kafkaStreamsSerde)
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.micrometer.registryPrometheus)
    implementation("no.nav.tms.varsel:kotlin-builder:1.1.0")
    testImplementation(libs.kafka.streams.test)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(project(":lib:kafka-key-generator-client"))
    testImplementation(project(":test:test-data-lib"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}


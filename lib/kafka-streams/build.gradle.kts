plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":lib:kafka"))
    compileOnly(libs.ktor.server.core)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams.core)
    implementation(libs.avro.kafkaStreamsSerde)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk)

    // Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

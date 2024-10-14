plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":lib:kafka"))
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams.core)
    implementation(libs.avro.kafkaStreamsSerde)

    // Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

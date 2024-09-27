plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":lib:kafka"))
    implementation(libs.kafkaClients)
    implementation(libs.kafkaStreams)
    implementation(libs.kafkaStreamsAvroSerde)

    // Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

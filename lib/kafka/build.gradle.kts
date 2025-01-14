plugins {
    kotlin("jvm")
}


dependencies {
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.coroutinesCore)
    compileOnly(libs.avro.kafkaSerializer)
    implementation(libs.kafka.clients)

    // Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

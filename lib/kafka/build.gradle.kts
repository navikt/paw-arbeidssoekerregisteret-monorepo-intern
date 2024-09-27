plugins {
    kotlin("jvm")
}


dependencies {
    compileOnly(libs.coroutinesCore)
    compileOnly(libs.kafkaSerializer)
    implementation(libs.kafkaClients)

    // Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm")
}


dependencies {
    api(project(":lib:async"))
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

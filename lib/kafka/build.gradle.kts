plugins {
    kotlin("jvm")
}


dependencies {
    api(project(":lib:async"))
    api(project(":lib:logging"))
    api(project(":lib:health"))
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.avro.kafkaSerializer)
    implementation(libs.kafka.clients)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

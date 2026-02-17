plugins {
    kotlin("jvm")
}


dependencies {
    api(project(":lib:logging"))
    api(project(":lib:kafka"))
    implementation(libs.kafka.clients)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.micrometer.core)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

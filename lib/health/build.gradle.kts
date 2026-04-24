plugins {
    kotlin("jvm")
}

dependencies {

    implementation(libs.ktor.server.core)
    compileOnly(libs.kafka.streams.core)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kafka.streams.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm")
}

dependencies {

    implementation(libs.ktor.server.core)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.test.host)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

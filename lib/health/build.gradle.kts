plugins {
    kotlin("jvm")
}

dependencies {

    implementation(libs.ktor.server.core)

    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.ktor.server.test.host)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

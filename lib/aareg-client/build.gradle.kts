plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val jvmMajorVersion: String by project

dependencies {
    api(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.ktor.client.mock)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

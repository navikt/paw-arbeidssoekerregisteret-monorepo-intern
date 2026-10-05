plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project
val jvmVersion = JavaVersion.valueOf("VERSION_$jvmMajorVersion")

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:serialization"))
    implementation(libs.ktor.serialization.jackson3)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.cio)
    api(libs.nav.common.tokenClient)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

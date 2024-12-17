plugins {
    kotlin("jvm")
}
val jvmMajorVersion: String by project
val jvmVersion = JavaVersion.valueOf("VERSION_$jvmMajorVersion")

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.ktor3.client.contentNegotiation)
    implementation(libs.ktor3.client.core)
    implementation(libs.ktor3.client.cio)
    implementation(libs.ktor3.serialization.jackson)
    implementation(libs.nav.security.tokenClientCore)
    api(libs.nav.common.tokenClient)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

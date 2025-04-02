plugins {
    kotlin("jvm")
}
val jvmMajorVersion: String by project
val jvmVersion = JavaVersion.valueOf("VERSION_$jvmMajorVersion")

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:error-handling"))
    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.nav.security.tokenClientCore)
    api(libs.nav.common.tokenClient)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

dependencies {
    api(libs.atlassian.oai.swaggerRequestValidator.core)
    implementation(libs.ktor.client.core)
}

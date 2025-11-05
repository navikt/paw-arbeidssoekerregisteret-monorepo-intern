plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    compileOnly(libs.ktor.client.core)
    compileOnly(libs.jackson.kotlin)
    compileOnly(libs.logback.classic)

    //Test
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.jackson.kotlin)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.logback.classic)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
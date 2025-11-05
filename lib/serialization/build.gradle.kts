plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    compileOnly(project(":lib:hoplite-config"))
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.ktor.server.contentNegotiation)
    compileOnly(libs.ktor.serialization.jackson)
    compileOnly(libs.jackson.kotlin)
    compileOnly(libs.jackson.datatype.jsr310)
    compileOnly(libs.kafka.clients)

    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
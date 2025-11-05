plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    compileOnly(project(":lib:hoplite-config"))
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.ktor.server.callId)
    compileOnly(libs.ktor.server.callLogging)
    compileOnly(libs.logback.classic)
    compileOnly(libs.logstash.logback.encoder)
    compileOnly(libs.nav.common.log)
    compileOnly(libs.nav.common.auditLog)

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
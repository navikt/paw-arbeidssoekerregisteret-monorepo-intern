plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    testImplementation(libs.bundles.unit.testing.kotest)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
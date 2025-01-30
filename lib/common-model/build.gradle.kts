plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
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
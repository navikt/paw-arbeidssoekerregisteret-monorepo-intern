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
    api(libs.kafka.clients)

    testImplementation(libs.bundles.unit.testing.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

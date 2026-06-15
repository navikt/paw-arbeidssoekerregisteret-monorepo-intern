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
    api(project(":lib:hoplite-config"))
    api(project(":lib:kafka"))

    testImplementation(libs.bundles.unit.testing.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

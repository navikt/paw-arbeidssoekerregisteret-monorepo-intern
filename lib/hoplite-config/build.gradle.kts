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
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.toml)

    testImplementation(libs.bundles.unit.testing.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Simulerer variabler Nais setter, som kolliderer med config-nøkler (port, hostname)
    environment("PORT", "8080")
    environment("HOSTNAME", "nais-pod")
    environment("TEST_DB_PORT", "5432")
    environment("TEST_DB_HOST", "db.example")
}

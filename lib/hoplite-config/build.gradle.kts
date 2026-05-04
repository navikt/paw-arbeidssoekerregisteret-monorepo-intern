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
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

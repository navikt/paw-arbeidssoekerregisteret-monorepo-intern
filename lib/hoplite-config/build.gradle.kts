plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.toml)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

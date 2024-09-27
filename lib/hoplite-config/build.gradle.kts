plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.hopliteCore)
    implementation(libs.hopliteToml)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

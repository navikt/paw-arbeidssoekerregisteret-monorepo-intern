plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.nav.common.log)
    implementation(libs.opentelemetry.annotations)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

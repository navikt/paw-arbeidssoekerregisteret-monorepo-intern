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
    implementation(libs.nav.common.log)
    implementation(libs.opentelemetry.annotations)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

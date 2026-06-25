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
    compileOnly(libs.kafka.streams.core)
    implementation(libs.logback.classic)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)
    implementation(project(":lib:logging"))

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.kafka.streams.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

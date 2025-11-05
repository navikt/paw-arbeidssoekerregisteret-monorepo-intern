import org.gradle.kotlin.dsl.dependencies

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":domain:error"))
    implementation(project(":lib:tracing"))
    compileOnly(project(":lib:kafka"))
    compileOnly(libs.ktor.server.cors)
    compileOnly(libs.ktor.server.statusPages)
    compileOnly(libs.ktor.serialization.jackson)
    compileOnly(libs.jackson.datatype.jsr310)
    compileOnly(libs.kafka.streams.core)
    compileOnly(libs.logback.classic)
    compileOnly(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.annotations)

    //Test
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.server.contentNegotiation)
    testImplementation(libs.ktor.server.statusPages)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.ktor.client.contentNegotiation)
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.kafka.streams.core)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.logback.classic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

import org.gradle.kotlin.dsl.dependencies

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":domain:felles"))
    implementation(project(":lib:error-handling"))
    implementation(libs.ktor.server.auth)
    implementation(libs.logback.classic)
    implementation(libs.nav.security.tokenValidationKtorV3)
    implementation(libs.ktor.serialization.jackson)

    //Test
    testImplementation(project(":lib:pdl-client"))
    testImplementation(project(":lib:serialization"))
    testImplementation(libs.nav.security.mockOauth2Server)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.contentNegotiation)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.jackson.datatype.jsr310)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

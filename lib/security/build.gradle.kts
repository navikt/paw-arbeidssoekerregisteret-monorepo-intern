plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:common-model"))
    implementation(project(":lib:logging"))
    implementation(libs.ktor.server.auth)
    implementation(libs.logbackClassic)
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
    testImplementation(libs.jackson.datatypeJsr310)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

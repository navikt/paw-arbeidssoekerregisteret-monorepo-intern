plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":lib:error-handling-ktor3"))
    implementation(libs.ktor3.server.auth)
    implementation(libs.logbackClassic)
    implementation(libs.nav.security.tokenValidationKtorV3)

    //Test
    testImplementation(project(":lib:pdl-client"))
    testImplementation(libs.nav.poao.tilgangClient)
    testImplementation(libs.nav.security.mockOauth2Server)
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.ktor3.server.test.host)
    testImplementation(libs.ktor3.client.contentNegotiation)
    testImplementation(libs.ktor3.serialization.jackson)
    testImplementation(libs.jackson.datatypeJsr310)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

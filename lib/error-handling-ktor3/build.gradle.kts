plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.ktor3.server.cors)
    compileOnly(libs.ktor3.serialization.jackson)
    compileOnly(libs.kafka.streams.core)
    compileOnly(libs.logbackClassic)

    //Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.ktor3.server.test.host)
    testImplementation(libs.ktor3.server.contentNegotiation)
    testImplementation(libs.ktor3.server.statusPages)
    testImplementation(libs.ktor3.serialization.jackson)
    testImplementation(libs.ktor3.client.contentNegotiation)
    testImplementation(libs.ktor3.server.core)
    testImplementation(libs.kafka.streams.core)
    testImplementation(libs.jackson.datatypeJsr310)
    testImplementation(libs.logbackClassic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.ktorServerCors)
    compileOnly(libs.ktorSerializationJackson)
    compileOnly(libs.kafkaStreams)
    compileOnly(libs.logbackClassic)

    //Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.ktorServerTestsJvm)
    testImplementation(libs.ktorServerContentNegotiation)
    testImplementation(libs.ktorServerStatusPages)
    testImplementation(libs.ktorSerializationJackson)
    testImplementation(libs.ktorClientContentNegotiation)
    testImplementation(libs.ktorServerCore)
    testImplementation(libs.kafkaStreams)
    testImplementation(libs.logbackClassic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

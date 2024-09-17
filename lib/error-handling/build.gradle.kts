plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(ktorServer.core)
    compileOnly(ktor.serializationJackson)
    compileOnly(orgApacheKafka.kafkaStreams)
    compileOnly(loggingLibs.logbackClassic)

    // Test
    testImplementation(testLibs.bundles.withUnitTesting)
    testImplementation(ktorServer.testJvm)
    testImplementation(ktorServer.contentNegotiation)
    testImplementation(ktorServer.statusPages)
    testImplementation(ktor.serializationJackson)
    testImplementation(ktorClient.contentNegotiation)
    testImplementation(ktorServer.core)
    testImplementation(orgApacheKafka.kafkaStreams)
    testImplementation(loggingLibs.logbackClassic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

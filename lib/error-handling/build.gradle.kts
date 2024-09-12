plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(ktorServer.core)
    compileOnly(orgApacheKafka.kafkaStreams)
    compileOnly(loggingLibs.logbackClassic)

    // Test
    testImplementation(testLibs.bundles.withUnitTesting)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

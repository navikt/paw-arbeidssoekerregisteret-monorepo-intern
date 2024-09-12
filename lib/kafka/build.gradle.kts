plugins {
    kotlin("jvm")
}


dependencies {
    compileOnly(kotlinx.coroutinesCore)
    compileOnly(apacheAvro.kafkaSerializer)
    implementation(orgApacheKafka.kafkaClients)

    // Test
    testImplementation(testLibs.bundles.withUnitTesting)
}


//tasks.withType<Test>().configureEach {
//    useJUnitPlatform()
//}

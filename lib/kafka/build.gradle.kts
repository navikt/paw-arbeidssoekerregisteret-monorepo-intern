plugins {
    kotlin("jvm")
}


dependencies {
    compileOnly(kotlinx.coroutinesCore)
    implementation(orgApacheKafka.kafkaClients)
    compileOnly(apacheAvro.kafkaSerializer)

    // Test
    testImplementation(testLibs.runnerJunit5)
    testImplementation(testLibs.assertionsCore)
}


//tasks.withType<Test>().configureEach {
//    useJUnitPlatform()
//}

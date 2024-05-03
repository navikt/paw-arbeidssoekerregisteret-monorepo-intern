plugins {
    kotlin("jvm")
}

val koTestVersion = "5.7.2"

dependencies {
    api(project(":lib:kafka"))
    implementation("org.apache.kafka:kafka-clients:3.6.0")
    implementation("org.apache.kafka:kafka-streams:3.6.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.4.0")

    // Test
    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$koTestVersion")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

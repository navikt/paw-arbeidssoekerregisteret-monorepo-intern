import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro")
}

dependencies {
    api(libs.avro.core)
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source("src/main/avro")
}

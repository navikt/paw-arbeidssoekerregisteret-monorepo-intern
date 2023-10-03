plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.7.0"
}

dependencies {
    implementation("org.apache.avro:avro:1.11.0")
}
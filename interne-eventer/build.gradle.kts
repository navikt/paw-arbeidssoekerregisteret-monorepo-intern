plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.8.0"
}

dependencies {
    implementation("org.apache.avro:avro:1.11.1")
    implementation("org.apache.avro:avro-ipc:1.11.1")
}

avro {
    setCreateSetters(false)
}
plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

dependencies {
    implementation(libs.kafka.streams.test)
    implementation(libs.test.kotest.assertionsCore)
    compileOnly(libs.avro.kafkaStreamsSerde)
}

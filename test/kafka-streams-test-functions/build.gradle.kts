plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.kafka.streams.test)
    implementation(libs.test.kotest.assertionsCore)
}

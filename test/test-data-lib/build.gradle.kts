import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":lib:kafka-key-generator-client"))

    implementation(libs.coroutinesCore)
    implementation(libs.avro.core)
}

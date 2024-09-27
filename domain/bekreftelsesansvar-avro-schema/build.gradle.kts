import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro")
}

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema(libs.bekreftelsesansvarSchema)
    implementation(libs.bekreftelsesansvarSchema)
    api(libs.avro)
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}

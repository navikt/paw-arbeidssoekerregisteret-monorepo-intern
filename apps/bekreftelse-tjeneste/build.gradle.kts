import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:bekreftelsesansvar-avro-schema"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))
    implementation(libs.kafkaStreams)
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.jacksonKotlin)
    implementation(libs.kafkaStreamsAvroSerde)
    implementation(libs.arrowCore)
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    testImplementation(libs.streamsTest)
    testImplementation(libs.runnerJunit5)
    testImplementation(libs.assertionsCore)
}

//enable context receiver
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":lib:kafka-key-generator-client"))

    implementation(kotlinx.coroutinesCore)
    implementation(apacheAvro.avro)
}

//enable context receiver
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

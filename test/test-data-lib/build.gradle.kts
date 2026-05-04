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
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))
    implementation(project(":domain:bekreftelse-paavegneav-avro-schema"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":lib:kafka-key-generator-client"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.avro.core)
}

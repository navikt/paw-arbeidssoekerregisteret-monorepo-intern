plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))
    implementation(project(":domain:bekreftelse-paavegneav-avro-schema"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":lib:kafka-key-generator-client"))

    implementation(libs.coroutinesCore)
    implementation(libs.avro.core)
}

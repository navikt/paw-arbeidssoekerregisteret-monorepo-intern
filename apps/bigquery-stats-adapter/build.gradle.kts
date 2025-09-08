plugins {
    kotlin("jvm")
    id("jib-chainguard")
    application
}

val jvmMajorVersion: String by project

dependencies {
    // Project
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:error-handling"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:bekreftelse-interne-hendelser"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))
    implementation(project(":domain:bekreftelse-paavegneav-avro-schema"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:metrics"))

    //Database
    implementation(platform(libs.database.bigquery.librariesBom))
    implementation(libs.database.bigquery.client)

    // Server
    implementation(libs.bundles.ktor.server.instrumented)

    // Serialization
    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.avro.core)
    implementation(libs.avro.kafkaSerializer)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.nav.common.log)

    // Instrumentation
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.annotations)

    // Testing
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(project(":test:test-data-lib"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.bqadapter.AppKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed")
    }
}

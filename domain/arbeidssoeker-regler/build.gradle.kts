plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    application
}

val jvmMajorVersion: String by project

dependencies {
    api(project(":lib:common-model"))
    implementation(project(":lib:pdl-client"))
    implementation(project(":domain:interne-hendelser"))
    api(libs.micrometer.registryPrometheus)
    api(libs.arrow.core.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
    testImplementation(libs.test.testContainers.core)
    testImplementation(libs.nav.security.mockOauth2Server)
    testImplementation(libs.test.mockk.core)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.ApplicationKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    application
}

val jvmMajorVersion: String by project

dependencies {
    api(project(":domain:felles"))
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":lib:pdl-client"))
    api(libs.arrow.core.core)
    implementation(libs.micrometer.registry.prometheus)
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

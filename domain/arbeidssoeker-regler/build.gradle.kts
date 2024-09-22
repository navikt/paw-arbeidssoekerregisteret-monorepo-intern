import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    application
    id("com.google.cloud.tools.jib")
}

val baseImage: String by project
val jvmMajorVersion: String by project

dependencies {
    implementation(pawClients.pawPdlClient)
    implementation(project(":domain:interne-hendelser"))
    api(micrometer.registryPrometheus)
    api(arrow.core)
    testImplementation(ktorServer.testJvm)
    testImplementation(testLibs.runnerJunit5)
    testImplementation(testLibs.assertionsCore)
    testImplementation(testLibs.testContainers)
    testImplementation(testLibs.mockOauth2Server)
    testImplementation(testLibs.mockk)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.ApplicationKt")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

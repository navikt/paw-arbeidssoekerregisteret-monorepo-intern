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
    implementation(project(":domain:interne-hendelser"))
    implementation(libs.pawPdlClient)
    api(libs.micrometerRegistryPrometheus)
    api(libs.arrowCore)
    testImplementation(libs.ktorServerTestsJvm)
    testImplementation(libs.runnerJunit5)
    testImplementation(libs.assertionsCore)
    testImplementation(libs.testContainers)
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.mockk)
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

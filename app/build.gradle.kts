plugins {
    kotlin("jvm") version "1.9.10"
    application
}

dependencies {

}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("no.nav.paw.kafkakeygenerator.AppKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
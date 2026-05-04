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
    api(project(":lib:hoplite-config"))
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

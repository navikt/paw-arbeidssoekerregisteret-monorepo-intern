plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.core)
    compileOnly(libs.paw.kafkaClients)

    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
    testImplementation(libs.paw.kafkaClients)
}

val jvmMajorVersion: String by project

group = "no.nav.paw.arbeidssokerregisteret.internt.schema"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
    repositories {
        maven {
            val mavenRepo: String by project
            val githubPassword: String by project
            setUrl("https://maven.pkg.github.com/navikt/$mavenRepo")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmMajorVersion)
    }
}

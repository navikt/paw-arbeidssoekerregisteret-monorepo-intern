plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(libs.jacksonDatatypeJsr310)
    compileOnly(libs.jacksonKotlin)
    compileOnly(libs.jacksonCore)
    compileOnly(libs.kafkaClients)

    testImplementation(libs.runnerJunit5)
    testImplementation(libs.assertionsCore)
    testImplementation(libs.kafkaClients)
    testImplementation(libs.jacksonDatatypeJsr310)
    testImplementation(libs.jacksonKotlin)
    testImplementation(libs.jacksonCore)
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

tasks.named<Test>("test") {
    useJUnitPlatform()
}


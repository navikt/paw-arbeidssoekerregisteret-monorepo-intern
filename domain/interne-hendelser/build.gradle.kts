plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(jackson.datatypeJsr310)
    compileOnly(jackson.kotlin)
    compileOnly(jackson.core)
    compileOnly(orgApacheKafka.kafkaClients)

    testImplementation(testLibs.runnerJunit5)
    testImplementation(testLibs.assertionsCore)
    testImplementation(orgApacheKafka.kafkaClients)
    testImplementation(jackson.datatypeJsr310)
    testImplementation(jackson.kotlin)
    testImplementation(jackson.core)
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


plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.jacksonKotlin)
    implementation(libs.jacksonCore)
    compileOnly(libs.kafkaClients)

    testImplementation(libs.runnerJunit5)
    testImplementation(libs.assertionsCore)
    testImplementation(libs.kafkaClients)
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

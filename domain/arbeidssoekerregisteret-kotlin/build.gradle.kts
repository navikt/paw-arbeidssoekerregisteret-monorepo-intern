plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":domain:interne-hendelser"))
    compileOnly(libs.jacksonDatatypeJsr310)
    compileOnly(libs.jacksonKotlin)
}

val jvmMajorVersion: String by project

group = "no.nav.paw.arbeidssokerregisteret.api.schema"

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

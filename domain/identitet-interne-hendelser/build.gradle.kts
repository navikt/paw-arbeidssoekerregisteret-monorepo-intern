plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.core)
    compileOnly(libs.kafka.clients)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.kafka.clients)
}

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
    val jvmMajorVersion: String by project
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmMajorVersion)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":interne-eventer"))
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
}

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

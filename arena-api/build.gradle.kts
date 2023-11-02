plugins {
    kotlin("jvm")
    `maven-publish`
}

val schemaDeps by configurations.creating {
    isTransitive = false
}

dependencies {
    schemaDeps(project(":eksternt-api"))
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

tasks.withType(Copy::class).configureEach {
    from("$rootDir/eksternt-api/src/main/resources/") {
        include("*.avdl", "vo/*.avdl")
        filter { line ->
            line.replace(
                "@namespace(\"no.nav.paw.arbeidssokerregisteret.api.v1\"",
                "@namespace(\"no.nav.paw.arbeidssokerregisteret.arena.v1\""
            )
        }
    }
    into("$buildTreePath/resources/main/")
}


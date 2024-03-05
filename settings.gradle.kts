rootProject.name = "paw-arbeidssokerregisteret-api-inngang"

dependencyResolutionManagement {
    val githubPassword: String by settings
    repositories {
        maven {
            setUrl("https://maven.pkg.github.com/navikt/*")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
    versionCatalogs {
        create("pawObservability") {
            from("no.nav.paw.observability:observability-version-catalog:23.10.25.8-1")
        }
    }
}

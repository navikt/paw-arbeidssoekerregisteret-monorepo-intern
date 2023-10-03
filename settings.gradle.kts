rootProject.name = "paw-arbeidssokerregisteret"

dependencyResolutionManagement {
    val githubPassword: String by settings
    repositories {
        maven {
            setUrl("https://maven.pkg.github.com/navikt/paw-observability")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        mavenLocal()
    }
    versionCatalogs {
        create("pawObservability") {
            from("no.nav.paw.observability:observability-version-catalog:23.09.22.7-1")
        }
    }
}

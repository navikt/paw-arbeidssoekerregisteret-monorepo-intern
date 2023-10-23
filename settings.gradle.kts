plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "paw-kafka-key-generator"
include("app")

dependencyResolutionManagement {
    val githubPassword: String by settings
    repositories {
        gradlePluginPortal()
        maven {
            setUrl("https://maven.pkg.github.com/navikt/paw-observability")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        mavenCentral()
    }
    versionCatalogs {
        create("pawObservability") {
            from("no.nav.paw.observability:observability-version-catalog:23.09.22.7-1")
        }
    }
}
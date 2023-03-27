rootProject.name = "aareg-client"

pluginManagement {
    val kotlinVersion: String by settings
    val kotlinterVersion: String by settings
    val sonarqubeVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jmailen.kotlinter") version kotlinterVersion
        id("maven-publish")
        id("org.sonarqube") version sonarqubeVersion
    }
}

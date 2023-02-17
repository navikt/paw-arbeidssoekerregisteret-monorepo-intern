rootProject.name = "pdl-client"

pluginManagement {
    val kotlinVersion: String by settings
    val kotlinterVersion: String by settings
    val graphQLKotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jmailen.kotlinter") version kotlinterVersion
        id("com.expediagroup.graphql") version graphQLKotlinVersion
        id("maven-publish")
    }
}

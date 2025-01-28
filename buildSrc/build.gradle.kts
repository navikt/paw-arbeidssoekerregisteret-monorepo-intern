plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.4.4")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
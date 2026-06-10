plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.google.cloud.tools.jib:com.google.cloud.tools.jib.gradle.plugin:3.5.3")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
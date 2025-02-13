plugins {
    kotlin("jvm")
    id("jib-distroless")
    application
}

val jvmMajorVersion: String by project

dependencies {
    // Project
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:logging"))
    implementation(project(":lib:database"))
    implementation(project(":lib:http-client-utils"))

    // Logging
    implementation(libs.nav.common.log)

    // Jackson
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.dataformat.csv)

    // Database
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.database.hikari.connectionPool)
    implementation(libs.database.postgres.driver)
    implementation(libs.database.flyway.postgres)

    // Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.test.testContainers.postgresql)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssoeker.synk.JobKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

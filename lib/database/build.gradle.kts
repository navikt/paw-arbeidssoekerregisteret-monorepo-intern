plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.database.hikari.connectionPool)
    compileOnly(libs.exposed.jdbc)
    compileOnly(libs.database.flyway.postgres)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":lib:async"))
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.kotlinx.coroutines.core)

    // Test
    testImplementation(libs.bundles.testLibsWithUnitTesting)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

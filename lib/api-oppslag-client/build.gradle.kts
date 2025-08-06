plugins {
    kotlin("jvm")
    id("org.openapi.generator")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":lib:logging"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:http-client-utils"))

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.jackson)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.client.mock)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.compileKotlin {
    dependsOn(tasks.openApiValidate, tasks.openApiGenerate)
}

tasks.compileTestKotlin {
    dependsOn(tasks.openApiValidate, tasks.openApiGenerate)
}

openApiValidate {
    inputSpec = "${layout.projectDirectory}/src/main/resources/openapi/api-oppslag.yaml"
}

openApiGenerate {
    generatorName.set("kotlin")
    library = "jvm-ktor"
    inputSpec = "${layout.projectDirectory}/src/main/resources/openapi/api-oppslag.yaml"
    outputDir = "${layout.buildDirectory.get()}/generated/"
    packageName = "no.nav.paw.client.api.oppslag"
    configOptions.set(
        mapOf(
            "serializationLibrary" to "jackson",
            "enumPropertyNaming" to "original",
        ),
    )
    typeMappings = mapOf(
        "DateTime" to "Instant"
    )
    globalProperties = mapOf(
        "apis" to "none",
        "models" to ""
    )
    importMappings = mapOf(
        "Instant" to "java.time.Instant"
    )
}

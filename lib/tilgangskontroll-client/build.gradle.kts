import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm")
}
val jvmMajorVersion: String by project
val jvmVersion = JavaVersion.valueOf("VERSION_$jvmMajorVersion")

dependencies {
    implementation(project(":lib:hoplite-config"))
    api(project(":lib:http-client-utils"))
    api(project(":lib:common-model"))
    api(project(":lib:error-handling"))
    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.nav.security.tokenClientCore)
    api(libs.nav.common.tokenClient)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}
val generatedServerCodePackageName = "no.nav.paw.tilgangskontroll.server"
val generatedCodeOutputDir = "${layout.buildDirectory.get()}/generated/"

mapOf(
    "${rootProject.allprojects.find { it.name == "tilgangskontroll" }?.layout?.projectDirectory}/src/main/resources/openapi/tilgangskontroll.yaml" to generatedServerCodePackageName
).map { (openApiDocFile, pkgName) ->
    val taskName = "generate${pkgName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
    tasks.register(taskName, GenerateTask::class) {
        generatorName.set("kotlin")
        library = "jvm-ktor"
        inputSpec = openApiDocFile
        outputDir = generatedCodeOutputDir
        packageName = pkgName
        configOptions.set(
            mapOf(
                "serializationLibrary" to "jackson",
                "enumPropertyNaming" to "original",
                "modelPropertyNaming" to "original"
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
    taskName
}.also { generatorTasks ->
    tasks.withType(KotlinCompilationTask::class) {
        dependsOn(*generatorTasks.toTypedArray())
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

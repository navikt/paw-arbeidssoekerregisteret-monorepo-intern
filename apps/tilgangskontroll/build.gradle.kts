import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    application
    id("jib-chainguard")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:error-handling"))

    //implementation(libs.arrow.core.core)
    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.toml)
    implementation(libs.nav.security.tokenValidationKtorV3)
    implementation(libs.nav.security.tokenClientCore)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.log)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.jvm)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.ktor.server.coreJvm)
    implementation(libs.ktor.server.openapi)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
    testImplementation(libs.nav.security.mockOauth2Server)
    testImplementation(libs.test.mockk.core)
    testImplementation(project(":lib:tilgangskontroll-client"))
}
sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.tilgangskontroll.StartAppKt")
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

val generatedCodePackageName = "no.nav.paw.tilgangskontroll.api"
val generatedCodeOutputDir = "${layout.buildDirectory.get()}/generated/"

mapOf(
    "${layout.projectDirectory}/src/main/resources/openapi/tilgangskontroll.yaml" to generatedCodePackageName
).map { (openApiDocFile, pkgName) ->
    val taskName = "generate${pkgName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
    tasks.register(taskName, GenerateTask::class) {
        generatorName.set("kotlin-server")
        library = "ktor"
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

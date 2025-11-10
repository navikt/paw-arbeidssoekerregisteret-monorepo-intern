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
    implementation(project(":domain:felles"))
    implementation(project(":domain:arbeidssoekerregisteret-kotlin"))
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":domain:arbeidssoeker-regler"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:pdl-client"))
    implementation(project(":lib:tilgangskontroll-client"))
    implementation(libs.arrow.functions)
    implementation(libs.arrow.integration.jackson)
    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.annotations)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.toml)
    implementation(libs.hoplite.yaml)
    implementation(libs.nav.security.tokenValidationKtorV3)
    implementation(libs.nav.security.tokenClientCore)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.log)
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.kafka.clients)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.callId)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.jvm)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.ktor.server.coreJvm)
    implementation(libs.ktor.server.openapi)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
    testImplementation(libs.test.testContainers.core)
    testImplementation(libs.nav.security.mockOauth2Server)
    testImplementation(libs.test.mockk.core)
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
    mainClass.set("no.nav.paw.arbeidssokerregisteret.ApplicationKt")
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

val generatedCodePackageName = "no.nav.paw.arbeidssoekerregisteret.api"
val generatedCodeOutputDir = "${layout.buildDirectory.get()}/generated/"

mapOf(
    "${layout.projectDirectory}/src/main/resources/openapi/opplysninger.yaml" to "${generatedCodePackageName}.opplysningermottatt",
    "${layout.projectDirectory}/src/main/resources/openapi/startstopp.yaml" to "${generatedCodePackageName}.startstopp",
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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    application
    id("com.google.cloud.tools.jib")
}

val baseImage: String by project
val jvmMajorVersion: String by project

val image: String? by project

dependencies {
    implementation(project(":domain:interne-hendelser"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-key-generator-client"))

    implementation(libs.arrow.core.core)
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.callId)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.jvm)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.nav.security.tokenValidationKtorV3)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.log)

    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.opentelemetry.annotations)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.toml)
    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.log)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.kafka.clients)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.database.postgres.driver)
    implementation(libs.database.flyway.core)
    implementation(libs.database.flyway.postgres)

    implementation(libs.jackson.datatypeJsr310)
    implementation(libs.jackson.kotlin)

    testImplementation(libs.test.junit5.runner)
    testImplementation(libs.test.kotest.assertionsCore)
    testImplementation(libs.test.mockk.core)
    testImplementation(libs.test.testContainers.core)
    testImplementation(libs.test.testContainers.postgresql)
    testImplementation(libs.ktor.server.test.host)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.backup.StartAppKt")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

jib {
    from.image = "$baseImage:$jvmMajorVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

val generatedCodePackageName = "no.nav.paw.arbeidssoekerregisteret.backup.api"
val generatedCodeOutputDir = "${layout.buildDirectory.get()}/generated/"

mapOf(
    "${layout.projectDirectory}/src/main/resources/openapi/Brukerstoette.yaml" to "${generatedCodePackageName}.brukerstoette"
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

mapOf(
    "${layout.projectDirectory}/src/main/resources/openapi/oppslags-api.yaml" to "${generatedCodePackageName}.oppslagsapi"
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
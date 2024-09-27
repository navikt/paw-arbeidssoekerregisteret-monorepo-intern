import org.gradle.configurationcache.extensions.capitalized
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

    implementation(libs.arrowCore)
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerSwagger)
    implementation(libs.ktorServerCallId)
    implementation(libs.ktorServerStatusPages)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorSerializationJvm)
    implementation(libs.ktorSerializationJackson)
    implementation(libs.tokenValidationKtorV2)
    implementation(libs.tokenClient)
    implementation(libs.tokenClient)
    implementation(libs.auditLog)
    implementation(libs.log)

    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.opentelemetryAnnotations)
    implementation(libs.hopliteCore)
    implementation(libs.hopliteToml)
    implementation(libs.auditLog)
    implementation(libs.log)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)
    implementation(libs.kafkaClients)
    implementation(libs.exposedCore)
    implementation(libs.exposedJdbc)
    implementation(libs.exposedJavaTime)
    implementation(libs.postgresDriver)
    implementation(libs.flywayCore)
    implementation(libs.flywayPostgres)

    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.jacksonKotlin)

    testImplementation(libs.runnerJunit5)
    testImplementation(libs.assertionsCore)
    testImplementation(libs.mockk)
    testImplementation(libs.testContainers)
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorServerTestsJvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.backup.StartAppKt")
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
    val taskName = "generate${pkgName.capitalized()}"
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
    val taskName = "generate${pkgName.capitalized()}"
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
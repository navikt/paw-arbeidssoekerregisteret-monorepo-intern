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

    implementation(ktorServer.bundles.withNettyAndMicrometer)
    implementation(ktorServer.cors)
    implementation(ktorServer.swagger)
    implementation(ktorServer.callId)
    implementation(ktorServer.statusPages)
    implementation(ktorServer.contentNegotiation)
    implementation(navSecurity.tokenValidationKtorV2)
    implementation(navSecurity.tokenClient)
    implementation(navCommon.tokenClient)
    implementation(navCommon.auditLog)
    implementation(navCommon.log)

    implementation(micrometer.registryPrometheus)
    implementation(otel.annotations)
    implementation(hoplite.hopliteCore)
    implementation(hoplite.hopliteToml)
    implementation(navCommon.auditLog)
    implementation(navCommon.log)
    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)
    implementation(orgApacheKafka.kafkaClients)
    implementation(exposed.core)
    implementation(exposed.jdbc)
    implementation(exposed.javaTime)
    implementation(postgres.driver)
    implementation(flyway.core)
    implementation(flyway.postgres)

    implementation(jackson.datatypeJsr310)
    implementation(jackson.kotlin)

    testImplementation(testLibs.runnerJunit5)
    testImplementation(testLibs.assertionsCore)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.testContainers)
    testImplementation(testLibs.postgresql)
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
        freeCompilerArgs.add("-Xcontext-receivers")
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
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    application
    id("com.google.cloud.tools.jib")
    id("io.ktor.plugin")
}

val baseImage: String by project
val jvmMajorVersion: String by project

val logbackVersion = "1.5.2"
val logstashVersion = "7.3"
val navCommonModulesVersion = "3.2024.02.21_11.18-8f9b43befae1"
val tokenSupportVersion = "3.1.5"
val koTestVersion = "5.7.2"
val arbeidssokerregisteretVersion = "24.03.25.160-1"
val pawUtilsVersion = "24.02.21.12-1"

val image: String? by project

dependencies {
    implementation("no.nav.paw.arbeidssokerregisteret.api.schema:arbeidssoekerregisteret-kotlin:$arbeidssokerregisteretVersion")
    implementation("no.nav.paw.arbeidssokerregisteret.internt.schema:interne-eventer:$arbeidssokerregisteretVersion")
    implementation(ktorServer.bundles.withNettyAndMicrometer)
    implementation(micrometer.registryPrometheus)
    implementation(otel.annotations)
    implementation(project(":lib:kafka"))
    implementation(project(":lib:hoplite-config"))
    implementation(hoplite.hopliteCore)
    implementation(hoplite.hopliteToml)
    implementation(hoplite.hopliteYaml)
    implementation(navSecurity.tokenValidationKtorV2)
    implementation(navSecurity.tokenClient)
    implementation(navCommon.tokenClient)
    implementation(navCommon.auditLog)
    implementation(navCommon.log)
    implementation("no.nav.poao-tilgang:client:2024.04.29_13.59-a0ddddd36ac9")
    implementation("no.nav.paw:pdl-client:24.01.12.26-1")

    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)
    implementation(orgApacheKafka.kafkaClients)

    implementation(ktorClient.contentNegotiation)
    implementation(ktorClient.core)
    implementation(ktorClient.cio)

    implementation(ktorServer.cors)
    implementation(ktorServer.swagger)
    implementation(ktorServer.callId)
    implementation(ktorServer.statusPages)
    implementation(ktorServer.contentNegotiation)

    //implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    //implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    //implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    //implementation("io.ktor:ktor-server-openapi:$ktorVersion")

    //testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$koTestVersion")
    testImplementation("org.testcontainers:testcontainers:1.19.6")
    testImplementation("no.nav.security:mock-oauth2-server:2.0.0")
    testImplementation("io.mockk:mockk:1.13.10")
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

jib {
    from.image = "$baseImage:$jvmMajorVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}",
            "OTEL_INSTRUMENTATION_METHODS_INCLUDE" to ("io.ktor.server.routing.Routing[interceptor,executeResult];" +
                "io.ktor.server.netty.NettyApplicationCallHandler[handleRequest,exceptionCaught];") +
            "io.ktor.serialization.jackson.JacksonConverter[deserialize,serializeNullable]"
        )
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
    }
}

val generatedCodePackageName = "no.nav.paw.arbeidssoekerregisteret.api"
val generatedCodeOutputDir = "${layout.buildDirectory.get()}/generated/"

mapOf(
    "${layout.projectDirectory}/src/main/resources/openapi/opplysninger.yaml" to "${generatedCodePackageName}.opplysningermottatt",
    "${layout.projectDirectory}/src/main/resources/openapi/startstopp.yaml" to "${generatedCodePackageName}.startstopp"
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

import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.9"
    id("org.openapi.generator") version "7.4.0"
    application
    id("com.google.cloud.tools.jib") version "3.4.0"
}

val jvmVersion = 21

val logbackVersion = "1.5.2"
val logstashVersion = "7.3"
val navCommonModulesVersion = "3.2024.02.21_11.18-8f9b43befae1"
val tokenSupportVersion = "3.1.5"
val koTestVersion = "5.7.2"
val hopliteVersion = "2.8.0.RC3"
val ktorVersion = pawObservability.versions.ktor
val arbeidssokerregisteretVersion = "24.03.25.160-1"
val pawUtilsVersion = "24.02.21.12-1"

val image: String? by project

dependencies {
    implementation("no.nav.paw.arbeidssokerregisteret.api.schema:arbeidssoekerregisteret-kotlin:$arbeidssokerregisteretVersion")
    implementation("no.nav.paw.arbeidssokerregisteret.internt.schema:interne-eventer:$arbeidssokerregisteretVersion")
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.common:token-client:$navCommonModulesVersion")
    implementation("no.nav.common:audit-log:$navCommonModulesVersion")
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("com.github.navikt.poao-tilgang:client:2024.03.04_10.19-63a652788672")
    implementation("no.nav.paw:pdl-client:24.01.12.26-1")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-toml:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")

    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion}")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-openapi:$ktorVersion")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
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
        languageVersion.set(JavaLanguageVersion.of(jvmVersion))
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
    from.image = "ghcr.io/navikt/baseimages/temurin:$jvmVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}",
            "OTEL_INSTRUMENTATION_METHODS_INCLUDE" to ("io.ktor.server.routing.Routing[interceptor,executeResult];" +
                "io.ktor.server.netty.NettyApplicationCallHandler[handleRequest,exceptionCaught]"),
            "JAVA_OPTS" to "-XX:+UseZGC -XX:+ZGenerational",
        )
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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("com.google.cloud.tools.jib")
}

val image: String? by project
val baseImage: String by project
val jvmMajorVersion: String by project

dependencies {
    // PAW
    implementation(project(":lib:hoplite-config"))
    implementation(libs.pawPdlClient)

    // NAV
    implementation(libs.log)
    implementation(libs.tokenClient)
    implementation(libs.tokenClientCore)
    implementation(libs.tokenValidationKtorV2)

    // Ktor
    implementation(libs.ktorSerializationJackson)

    // Ktor Server
    implementation(libs.bundles.ktorServerWithNettyAndMicrometer)
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerSwagger)
    implementation(libs.ktorServerCallId)
    implementation(libs.ktorServerStatusPages)
    implementation(libs.ktorServerContentNegotiation)

    // Ktor Client
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientOkhttp)
    implementation(libs.ktorClientLogging)

    // Micrometer & OTEL
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.opentelemetryApi)
    implementation(libs.opentelemetryAnnotations)
    implementation(libs.opentelemetryKtor)

    // Database
    implementation(libs.exposedCore)
    implementation(libs.exposedCrypt)
    implementation(libs.exposedDao)
    implementation(libs.exposedJdbc)
    implementation(libs.exposedJavaTime)
    implementation(libs.postgresDriver)
    implementation(libs.flywayCore)
    implementation(libs.flywayPostgres)
    implementation(libs.hikariConnectionPool)

    // Config
    implementation(libs.hopliteToml)

    // Logging
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

    // Tester
    testImplementation(libs.bundles.testLibsWithUnitTesting)
    testImplementation(libs.testContainers)
    testImplementation(libs.postgresql)
    testImplementation(libs.ktorServerTestsJvm)
    testImplementation(libs.ktorClientMock)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.kafkakeygenerator.AppStarterKt")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
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

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.create("runTestApp", JavaExec::class) {
    classpath = sourceSets["test"].runtimeClasspath +
            sourceSets["main"].runtimeClasspath
    mainClass = "no.nav.paw.kafkakeygenerator.Run_test_appKt"
    args = listOf()
}

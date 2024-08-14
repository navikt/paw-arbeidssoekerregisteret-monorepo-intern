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
    implementation(pawClients.pawPdlClient)

    // NAV
    implementation(navCommon.log)
    implementation(navCommon.tokenClient)
    implementation(navSecurity.tokenClient)
    implementation(navSecurity.tokenValidationKtorV2)

    // Ktor
    implementation(ktor.serializationJackson)

    // Ktor Server
    implementation(ktorServer.bundles.withNettyAndMicrometer)
    implementation(ktorServer.cors)
    implementation(ktorServer.swagger)
    implementation(ktorServer.callId)
    implementation(ktorServer.statusPages)
    implementation(ktorServer.contentNegotiation)

    // Ktor Client
    implementation(ktorClient.contentNegotiation)
    implementation(ktorClient.core)
    implementation(ktorClient.cio)
    implementation(ktorClient.okhttp)
    implementation(ktorClient.logging)

    // Micrometer & OTEL
    implementation(micrometer.registryPrometheus)
    implementation(otel.api)
    implementation(otel.annotations)
    implementation(otel.ktor)

    // Database
    implementation(exposed.core)
    implementation(exposed.crypt)
    implementation(exposed.dao)
    implementation(exposed.jdbc)
    implementation(exposed.javaTime)
    implementation(postgres.driver)
    implementation(flyway.core)
    implementation(flyway.postgres)
    implementation(hikari.connectionPool)

    // Config
    implementation(hoplite.hopliteToml)

    // Logging
    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)

    // Tester
    testImplementation(testLibs.bundles.withUnitTesting)
    testImplementation(testLibs.testContainers)
    testImplementation(testLibs.postgresql)
    testImplementation(ktorServer.testJvm)
    testImplementation(ktorClient.mock)
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

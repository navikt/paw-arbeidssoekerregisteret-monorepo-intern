import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0"
    application
    id("com.google.cloud.tools.jib") version "3.4.3"
}
val jvmVersion = 21
val image: String? by project


val exposedVersion = "0.52.0"
val logbackVersion = "1.5.2"
val logstashVersion = "7.4"
val navCommonModulesVersion = "3.2024.05.23_05.46-2b29fa343e8e"
val tokenSupportVersion = "5.0.1"
val ktorVersion = "2.3.12"

dependencies {
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")
    // Konfigurasjon
    implementation("com.sksamuel.hoplite:hoplite-core:2.8.0.RC3")
    implementation("com.sksamuel.hoplite:hoplite-toml:2.8.0.RC3")

    // NAV
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.common:token-client:$navCommonModulesVersion")
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.paw:pdl-client:24.07.04.39-1")

    // Database
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.flywaydb:flyway-core:9.21.2")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    //Otel
    implementation("io.opentelemetry:opentelemetry-api:1.39.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-2.0:2.4.0-alpha")

    // Micrometer
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // Tester
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.3")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.testcontainers:postgresql:1.19.8")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmVersion))
    }
}

application {
    mainClass.set("no.nav.paw.kafkakeygenerator.AppStarterKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

jib {
    from.image = "ghcr.io/navikt/baseimages/temurin:$jvmVersion"
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

tasks.create("runTestApp", JavaExec::class) {
    classpath = sourceSets["test"].runtimeClasspath +
            sourceSets["main"].runtimeClasspath
    mainClass = "no.nav.paw.kafkakeygenerator.Run_test_appKt"
    args = listOf()
}

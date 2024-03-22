import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.8"
    application
}
val exposedVersion = "0.48.0"
val logbackVersion = "1.5.2"
val logstashVersion = "7.4"
val navCommonModulesVersion = "3.2024.02.21_11.18-8f9b43befae1"
val tokenSupportVersion = "4.1.3"
val ktorVersion: Provider<String> = pawObservability.versions.ktor

dependencies {
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
    implementation("no.nav.paw:pdl-client:24.03.22.31-1")

    // Database
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.flywaydb:flyway-core:9.21.2")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Ktor
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")


    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // Tester
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.3")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.testcontainers:postgresql:1.19.7")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("no.nav.paw.kafkakeygenerator.AppStarterKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
}

tasks.create("runTestApp", JavaExec::class) {
    classpath = sourceSets["test"].runtimeClasspath +
            sourceSets["main"].runtimeClasspath
    mainClass = "no.nav.paw.kafkakeygenerator.Run_test_appKt"
    args = listOf()
}

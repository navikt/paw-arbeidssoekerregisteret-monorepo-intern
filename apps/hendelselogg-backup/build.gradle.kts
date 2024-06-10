import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("com.google.cloud.tools.jib")
}

val baseImage: String by project
val jvmMajorVersion: String by project

val image: String? by project

dependencies {
    implementation(project(":domain:interne-hendelser"))
    implementation(ktorServer.bundles.withNettyAndMicrometer)
    implementation(micrometer.registryPrometheus)
    implementation(otel.annotations)
    implementation(project(":lib:kafka"))
    implementation(project(":lib:hoplite-config"))
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

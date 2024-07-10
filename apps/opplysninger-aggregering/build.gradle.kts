import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    //id("io.ktor.plugin")
    id("org.openapi.generator")
    id("com.google.cloud.tools.jib")
    application
}

val jvmMajorVersion: String by project
val baseImage: String by project
val image: String? by project

val agents by configurations.creating

dependencies {
    // Project
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":domain:main-avro-schema"))

    // Server
    implementation(ktorServer.bundles.withNettyAndMicrometer)

    // Serialization
    implementation(ktor.serializationJackson)
    implementation(jackson.datatypeJsr310)

    // Logging
    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)
    implementation(navCommon.log)

    // Instrumentation
    implementation(micrometer.registryPrometheus)
    implementation(otel.api)
    implementation(otel.annotations)

    // Kafka
    implementation(orgApacheKafka.kafkaStreams)
    implementation(apacheAvro.kafkaStreamsAvroSerde)

    // Test
    testImplementation(ktorServer.testJvm)
    testImplementation(testLibs.bundles.withUnitTesting)
    testImplementation(testLibs.mockk)
    testImplementation(orgApacheKafka.streamsTest)

    agents(otel.javaagent)
}

application {
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.ApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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

tasks.register<Copy>("copyAgents") {
    from(agents)
    into("${layout.buildDirectory.get()}/agents")
}

tasks.named("assemble") {
    finalizedBy("copyAgents")
}

jib {
    from.image = "$baseImage:$jvmMajorVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}"
        )
        jvmFlags = listOf(
            "-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational"
        )
    }
}

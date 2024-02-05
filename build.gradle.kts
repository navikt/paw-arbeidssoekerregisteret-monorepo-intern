import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
//    id("org.jmailen.kotlinter") version "3.16.0"
    application
    id("com.google.cloud.tools.jib") version "3.4.0"
}

val jvmVersion = 21

val logbackVersion = "1.4.5"
val logstashVersion = "7.3"
val navCommonModulesVersion = "2.2023.01.02_13.51-1c6adeb1653b"
val tokenSupportVersion = "3.1.5"
val koTestVersion = "5.7.2"
val hopliteVersion = "2.8.0.RC3"
val ktorVersion = pawObservability.versions.ktor
val arbeidssokerregisteretVersion = "23.12.18.110-1"
val pawUtilsVersion = "24.01.11.9-1"

val image: String? by project

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    mavenNav("paw-arbeidssokerregisteret-api-inngang")
}

val agent by configurations.creating {
    isTransitive = false
}

val agentExtension by configurations.creating {
    isTransitive = false
}

dependencies {
    agent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.31.0")
    agentExtension("no.nav.paw.observability:opentelemetry-anonymisering-1.31.0:23.10.25.8-1")
    implementation("no.nav.paw.arbeidssokerregisteret.internt.schema:interne-eventer:$arbeidssokerregisteretVersion")
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.common:token-client:$navCommonModulesVersion")
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("com.github.navikt.poao-tilgang:client:2023.09.25_09.26-72043f243cad")
    implementation("no.nav.paw:pdl-client:0.3.7")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-toml:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")

    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion}")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    // TODO: Flytte til bundle KTOR

    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$koTestVersion")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.testcontainers:testcontainers:1.19.1")
    testImplementation("no.nav.security:mock-oauth2-server:2.0.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmVersion))
    }
}

val agentExtensionJar = "agent-extension.jar"
val agentJar = "agent.jar"
val agentsFolder = layout.buildDirectory.get().dir("agents").toString()

tasks.create("addAgent", Copy::class) {
    from(agent, agentExtension)
    into(agentsFolder)
    doFirst {
        delete(agentsFolder)
    }
    rename { name ->
        if (name.contains("anonymisering")) {
            agentExtensionJar
        } else {
            agentJar
        }
    }
 }

tasks.withType(KotlinCompile::class) {
    dependsOn.add("addAgent")
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
    to.image = "${image ?: project.name }:${project.version}"
    extraDirectories {
        paths {
            path {
                setFrom(agentsFolder)
                into = "/app"
            }
        }
    }
    container.entrypoint = listOf(
        "java",
        "-cp", "@/app/jib-classpath-file",
        "-javaagent:/app/$agentJar",
        "-Dotel.javaagent.extensions=/app/$agentExtensionJar",
        "-Dotel.resource.attributes=service.name=${project.name}",
        application.mainClass.get()
    )

    println("Container entrypoint: ${container.entrypoint}")
}

fun RepositoryHandler.mavenNav(repo: String): MavenArtifactRepository {
    val githubPassword: String by project

    return maven {
        setUrl("https://maven.pkg.github.com/navikt/$repo")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.4"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.7.0"
    id("org.jmailen.kotlinter") version "3.16.0"
    application
}

val logbackVersion = "1.4.5"
val logstashVersion = "7.3"
val navCommonModulesVersion = "2.2023.01.02_13.51-1c6adeb1653b"
val avroVersion = "1.11.0"
val tokenSupportVersion = "3.1.5"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    mavenNav("paw-arbeidssokerregisteret")
}

dependencies {
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.common:token-client:$navCommonModulesVersion")
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:7.4.0")
    implementation("com.github.navikt.poao-tilgang:client:2023.09.25_09.26-72043f243cad")
    implementation("no.nav.paw:pdl-client:0.3.1")

    // TODO: Flytte til bundle KTOR
    val ktorVersion = "2.3.4"
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    "run"(JavaExec::class) {
        jvmArgs = listOf(
            "-javaagent:agents/opentelemetry-javaagent.jar",
            "-Dotel.javaagent.extensions=agents/opentelemetry-anonymisering-1.30.0-23.09.22.7-1.jar",
            "-Dotel.resource.attributes=service.name=paw-arbeidssokerregisteret",
        )
        environment("KAFKA_BROKERS", "localhost:9092")
        environment("KAFKA_SCHEMA_REGISTRY", "http://localhost:8082")
        environment("KAFKA_PRODUCER_ID", "paw-consumer-arbeidssokerregisteret-v1")
        environment("KAFKA_PRODUCER_PERIODER_TOPIC", "paw.arbeidssokerperioder-v1")
        environment("KAFKA_GROUP_ID", "paw-consumer-arbeidssokerregisteret-v1")
        environment("AZURE_APP_WELL_KNOWN_URL", "http://localhost:8081/default/.well-known/openid-configuration")
        environment("AZURE_APP_CLIENT_ID", "paw-arbeidssokerregisteret")
        environment("TOKEN_X_WELL_KNOWN_URL", "http://localhost:8081/default/.well-known/openid-configuration")
        environment("TOKEN_X_CLIENT_ID", "paw-arbeidssokerregisteret")
        environment("POAO_TILGANG_CLIENT_URL", "http://localhost:8090/poao-tilgang")
        environment("POAO_TILGANG_CLIENT_SCOPE", "api://test.test.poao-tilgang/.default")
        environment("PDL_CLIENT_URL", "http://localhost:8090/pdl")
        environment("PDL_CLIENT_SCOPE", "api://test.test.pdl-api/.default")
        environment("AZURE_APP_CLIENT_ID", "paw-arbeidssokerregisteret")
        environment("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "http://localhost:8081/default/token")
        environment("OTEL_TRACES_EXPORTER", "maskert_oltp")
        environment("OTEL_METRICS_EXPORTER", "none")
        environment("OTEL_JAVAAGENT_DEBUG", "false")
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.ApplicationKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
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

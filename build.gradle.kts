plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.3"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.7.0"
    application
}

val exposedVersion = "0.42.1"
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
    mavenNav("paw-arbeidssoker-registeret")
}

dependencies {
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.flywaydb:flyway-core:9.21.2")
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("no.nav.common:log:2.2023.01.10_13.49-81ddc732df3a")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:7.4.0")

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
            "-javaagent:../agents/opentelemetry-javaagent.jar",
            "-Dotel.javaagent.extensions=../opentelemetry-agent-extension/build/libs/opentelemetry-agent-extension.jar",
            "-Dotel.resource.attributes=service.name=paw-arbeidssoker-registeret",
        )
        environment("NAIS_DATABASE_PAW_ARBEIDSSOKER_REGISTERET_ARBEIDSSOKERREGISTERET_PASSWORD", "admin")
        environment("NAIS_DATABASE_PAW_ARBEIDSSOKER_REGISTERET_ARBEIDSSOKERREGISTERET_USERNAME", "admin")
        environment("NAIS_DATABASE_PAW_ARBEIDSSOKER_REGISTERET_ARBEIDSSOKERREGISTERET_HOST", "localhost")
        environment("NAIS_DATABASE_PAW_ARBEIDSSOKER_REGISTERET_ARBEIDSSOKERREGISTERET_PORT", "5432")
        environment("NAIS_DATABASE_PAW_ARBEIDSSOKER_REGISTERET_ARBEIDSSOKERREGISTERET_DATABASE", "arbeidssokerregisteret")
        environment("KAFKA_BROKERS", "localhost:9092")
        environment("KAFKA_SCHEMA_REGISTRY", "http://localhost:8082")
        environment("KAFKA_PRODUCER_ID", "paw-consumer-arbeidssokerregisteret-v1")
        environment("KAFKA_PRODUCER_PERIODER_TOPIC", "paw.arbeidssokerperioder-v1")
        environment("KAFKA_GROUP_ID", "paw-consumer-arbeidssokerregisteret-v1")
        environment("IDPORTEN_WELL_KNOWN_URL", "http://localhost:8081/default/.well-known/openid-configuration")
        environment("IDPORTEN_CLIENT_ID", "paw-arbeidssoker-registeret")
        environment("OTEL_TRACES_EXPORTER", "maskert_oltp")
        environment("OTEL_METRICS_EXPORTER", "none")
        environment("OTEL_JAVAAGENT_DEBUG", "true")
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssookerregisteret.AppKt")
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

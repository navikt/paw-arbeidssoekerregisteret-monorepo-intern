plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.7.0"
    application
}
val logbackVersion = "1.4.5"
val logstashVersion = "7.3"
dependencies {
    implementation("no.nav.common:log:2.2023.01.10_13.49-81ddc732df3a")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation(project(":interne-eventer"))
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("org.apache.kafka:kafka-streams:3.5.1")
    implementation("org.apache.avro:avro:1.11.0")
    implementation("io.confluent:kafka-avro-serializer:7.4.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.4.0")
    implementation("org.apache.avro:avro:1.11.0")
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.app.AppKt")
}

tasks {
    "run"(JavaExec::class) {
        environment("KAFKA_STREAM_APPLICATION_ID", "arbeidssokerregisteret-eventlog-v2")
        environment("EVENTLOG_TOPIC", "input")
        environment("NAIS_DATABASE_PAW_DEMO_PAWDEMO_PASSWORD", "admin")
        environment("NAIS_DATABASE_PAW_DEMO_PAWDEMO_USERNAME", "admin")
        environment("NAIS_DATABASE_PAW_DEMO_PAWDEMO_HOST", "localhost")
        environment("NAIS_DATABASE_PAW_DEMO_PAWDEMO_PORT", "5432")
        environment("NAIS_DATABASE_PAW_DEMO_PAWDEMO_DATABASE", "pawdemo")
        environment("KAFKA_BROKERS", "localhost:9092")
        environment("KAFKA_SCHEMA_REGISTRY", "http://localhost:8082")
        environment("KAFKA_PRODUCER_ID", "paw-demo-v1")
        environment("KAFKA_PRODUCER_TOPIC", "paw.paw-demo-v1")
        environment("KAFKA_GROUP_ID", "PAW_DEMO_CONSUMER")
        environment("TOKEN_X_WELL_KNOWN_URL", "http://localhost:8081/default/.well-known/openid-configuration")
        environment("TOKEN_X_CLIENT_ID", "paw-demo")
        environment("OTEL_TRACES_EXPORTER", "maskert_oltp")
        environment("OTEL_METRICS_EXPORTER", "none")
        environment("OTEL_JAVAAGENT_DEBUG", "true")
    }
}
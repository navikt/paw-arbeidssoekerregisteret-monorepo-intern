package no.nav.paw.kafkakeymaintenance.pdlprocessor

import java.time.Duration

data class AktorConfig(
    val aktorTopic: String,
    val hendelseloggTopic: String,
    val supressionDelayMS: Long,
    val intervalMS: Long,
    val batchSize: Int = 200
) {
    companion object {
        val configFile: String get() = "aktor_topology_config.toml"
    }
}

val AktorConfig.supressionDelay: Duration get() = Duration.ofMillis(supressionDelayMS)
val AktorConfig.interval: Duration get() = Duration.ofMillis(intervalMS)
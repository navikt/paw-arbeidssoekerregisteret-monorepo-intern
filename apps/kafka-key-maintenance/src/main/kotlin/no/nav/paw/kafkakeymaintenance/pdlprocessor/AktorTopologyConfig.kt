package no.nav.paw.kafkakeymaintenance.pdlprocessor

import java.time.Duration

data class AktorTopologyConfig(
    val aktorTopic: String,
    val hendelseloggTopic: String,
    val supressionDelayMS: Long,
    val intervalMS: Long,
    val stateStoreName: String = "aktor_supression"
) {
    companion object {
        val configFile: String get() = "aktor_topology_config.toml"
    }
}

val AktorTopologyConfig.supressionDelay: Duration get() = Duration.ofMillis(supressionDelayMS)
val AktorTopologyConfig.interval: Duration get() = Duration.ofMillis(intervalMS)
package no.nav.paw.kafkakeymaintenance.pdlprocessor

import java.time.Duration

data class AktorTopologyConfig(
    val aktorTopic: String,
    val hendelseloggTopic: String,
    val supressionDelay: Duration,
    val interval: Duration,
    val stateStoreName: String = "aktor_supression"
) {
    companion object {
        val configFile: String get() = "aktor_topology_config.toml"
    }
}
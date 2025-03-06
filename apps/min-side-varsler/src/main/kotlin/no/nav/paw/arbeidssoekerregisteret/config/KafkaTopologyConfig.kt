package no.nav.paw.arbeidssoekerregisteret.config

import java.time.Duration

const val KAFKA_TOPICS_CONFIG = "kafka_topology_config.toml"

data class KafkaTopologyConfig(
    val shutdownTimeout: Duration = Duration.ofSeconds(5),
    val periodeStreamSuffix: String,
    val bekreftelseStreamSuffix: String,
    val varselHendelseStreamSuffix: String,
    val periodeTopic: String,
    val bekreftelseHendelseTopic: String,
    val tmsVarselTopic: String,
    val tmsVarselHendelseTopic: String
)

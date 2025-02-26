package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config

const val KAFKA_TOPICS_CONFIG = "kafka_topology_config.toml"

data class KafkaTopologyConfig(
    val bekreftelseStreamSuffix: String,
    val varselHendelseStreamSuffix: String,
    val periodeTopic: String,
    val bekreftelseHendelseTopic: String,
    val tmsVarselTopic: String,
    val tmsVarselHendelseTopic: String
)

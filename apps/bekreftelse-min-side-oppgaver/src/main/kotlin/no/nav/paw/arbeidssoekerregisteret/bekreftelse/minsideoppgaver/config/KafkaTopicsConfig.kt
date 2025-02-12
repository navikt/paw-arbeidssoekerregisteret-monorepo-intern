package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config

const val KAFKA_TOPICS_CONFIG = "kafka_topics_config.toml"

data class KafkaTopicsConfig(
    val periodeTopic: String,
    val bekreftelseHendelseTopic: String,
    val tmsOppgaveTopic: String,
    val tmsVarselHendelseTopic: String
)

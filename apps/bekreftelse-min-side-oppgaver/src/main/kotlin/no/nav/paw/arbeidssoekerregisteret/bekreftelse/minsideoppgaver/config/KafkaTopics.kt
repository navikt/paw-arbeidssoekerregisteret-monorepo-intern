package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config

import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

data class KafkaTopics(
    val periodeTopic: String,
    val tmsOppgaveTopic: String,
    val bekreftelseHendelseTopic: String
)

fun kafkaTopics(): KafkaTopics =
    loadNaisOrLocalConfiguration("kafka_topics.toml")
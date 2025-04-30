package no.nav.paw.arbeidssoekerregisteret.backup.config

data class ApplicationConfig(
    val version: Int,
    val antallPartitioner: Int,
    val consumerId: String,
    val consumerGroupId: String,
    val hendelsesloggTopic: String,
)
package no.nav.paw.bekreftelse.config

import no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning

const val APPLICATION_CONFIG = "application_config.toml"

data class ApplicationConfig(
    val kafkaTopology: KafkaTopologyConfig,
    val bekreftelseKlienter: List<BekreftelseKlient>
)

data class KafkaTopologyConfig(
    val bekreftelseTargetTopic: String,
    val bekreftelsePaaVegneAvTargetTopic: String
)

data class BekreftelseKlient(
    val versjon: Int,
    val bekreftelsesloesning: String,
    val paaVegneAvSourceTopic: String,
    val bekreftelseSourceTopic: String
) {
    init {
        val loesning = Bekreftelsesloesning.valueOf(bekreftelsesloesning.uppercase())
        require(loesning != Bekreftelsesloesning.UKJENT_VERDI) {
            "Bekreftelsesløsning kan ikke være 'UKJENT_VERDI'"
        }
    }
}

val BekreftelseKlient.bekreftelseApplicationIdSuffix: ApplicationIdSuffix
    get() = ApplicationIdSuffix("bekreftelse-${bekreftelsesloesning}-${versjon}")

val BekreftelseKlient.bekreftelsePaaVegneAvApplicationIdSuffix: ApplicationIdSuffix
    get() = ApplicationIdSuffix("bekreftelse-paavegneav-${bekreftelsesloesning}-${versjon}")

@JvmInline
value class ApplicationIdSuffix(val value: String)

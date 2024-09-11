package no.nav.paw.meldeplikttjeneste

import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstandSerde
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde

class ApplicationContext(
    val internTilstandSerde: InternTilstandSerde,
    val bekreftelseHendelseSerde: BekreftelseHendelseSerde,
    val kafkaKeysClient: KafkaKeysClient
) {
    val kafkaKeyFunction: (String) -> KafkaKeysResponse = {
        runBlocking { kafkaKeysClient.getIdAndKey(it) }
    }
}
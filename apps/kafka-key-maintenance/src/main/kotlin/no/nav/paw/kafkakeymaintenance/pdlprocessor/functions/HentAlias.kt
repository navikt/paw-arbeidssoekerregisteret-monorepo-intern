package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.LokaleAlias

fun KafkaKeysClient.hentAlias(antallPartisjoner: Int, identiteter: List<String>): List<LokaleAlias> = runBlocking {
    getAlias(antallPartisjoner, identiteter).alias
}
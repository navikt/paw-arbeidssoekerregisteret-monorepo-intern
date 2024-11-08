package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.ANTALL_PARTISJONER

fun KafkaKeysClient.hentAlias(identiteter: List<String>): List<LokaleAlias> = runBlocking {
    getAlias(ANTALL_PARTISJONER, identiteter).alias
}
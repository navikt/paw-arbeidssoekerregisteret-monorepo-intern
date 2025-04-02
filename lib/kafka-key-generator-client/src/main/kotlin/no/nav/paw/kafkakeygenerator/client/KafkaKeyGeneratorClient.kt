package no.nav.paw.kafkakeygenerator.client

import no.nav.paw.kafkakeygenerator.model.HentAliasResponse
import no.nav.paw.kafkakeygenerator.model.HentKeysResponse

interface KafkaKeyGeneratorClient {
    suspend fun hent(ident: String): HentKeysResponse?
    suspend fun hentEllerOpprett(ident: String): HentKeysResponse?
    suspend fun hentAlias(antallPartisjoner: Int, identer: List<String>): HentAliasResponse
}
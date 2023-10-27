package no.nav.paw.kafkakeygenerator.pdl

import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.TracePath
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.hentIdenter

private const val consumerId = "paw-arbeidssoekerregisteret"
class PdlIdentitesTjeneste(private val pdlKlient: PdlClient) {
    suspend fun hentIdentiter(
        tracePath: TracePath,
        identitet: Identitetsnummer
    ): List<String> {
        try {
            return pdlKlient.hentIdenter(identitet.value, tracePath.value, consumerId)
                ?.map { it.ident }
                ?: emptyList()
        } catch (ex: PdlException) {
            throw IdentitetstjenesteException("Feil ved henting av identiteter fra PDL", ex)
        }
    }
}

class IdentitetstjenesteException(message: String, årsak: Exception?) : Exception(message, årsak)
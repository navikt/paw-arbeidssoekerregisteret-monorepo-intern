package no.nav.paw.rapportering.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.PipelineContext
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.rapportering.api.services.AutorisasjonService
import no.nav.paw.rapportering.api.services.NavAnsatt
import no.nav.paw.rapportering.api.utils.getClaims
import no.nav.paw.rapportering.api.utils.getNAVident
import no.nav.paw.rapportering.api.utils.getOid
import no.nav.paw.rapportering.api.utils.getPid
import no.nav.paw.rapportering.api.utils.isAzure
import no.nav.poao_tilgang.client.TilgangType


context(PipelineContext<Unit, ApplicationCall>)
suspend fun requestScope(
    requestIdentitetsnummer: String?,
    autorisasjonService: AutorisasjonService,
    kafkaKeyClient: KafkaKeysClient,
    tilgangType: TilgangType
): Long {
    val claims = call.getClaims()
    val identitetsnummer = claims?.getPid()
        ?: requestIdentitetsnummer
        ?: throw IllegalArgumentException("Identitetsnummer mangler")

    if (claims?.isAzure == true) {
        val oid = claims.getOid()
        val navIdent = claims.getNAVident()
        val navAnsatt = NavAnsatt(
            azureId = oid,
            navIdent= navIdent
        )
        val tilgang = autorisasjonService.verifiserTilgangTilBruker(navAnsatt, identitetsnummer, tilgangType)
        if (!tilgang) {
            call.respond(HttpStatusCode.Forbidden)
            throw IllegalArgumentException("NAV-ansatt har ikke tilgang til bruker")
        }
    }

    val arbeidsoekerId = kafkaKeyClient.getIdAndKey(identitetsnummer)?.id
        ?: throw IllegalArgumentException("Fant ikke arbeidsoekerId for identitetsnummer")

    return arbeidsoekerId
}
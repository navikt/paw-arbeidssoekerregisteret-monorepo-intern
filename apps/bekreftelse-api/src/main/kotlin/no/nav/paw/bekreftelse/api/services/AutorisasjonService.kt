package no.nav.paw.bekreftelse.api.services

import no.nav.paw.bekreftelse.api.exception.BrukerHarIkkeTilgangException
import no.nav.paw.bekreftelse.api.utils.audit
import no.nav.paw.bekreftelse.api.utils.auditLogger
import no.nav.paw.bekreftelse.api.utils.logger
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.TilgangType
import java.util.*

class AutorisasjonService(
    private val poaoTilgangHttpClient: PoaoTilgangCachedClient
) {
    fun verifiserTilgangTilBruker(
        navAnsatt: NavAnsatt,
        identitetsnummer: String,
        tilgangType: TilgangType
    ) {
        val navAnsattTilgang = poaoTilgangHttpClient.evaluatePolicy(
            NavAnsattTilgangTilEksternBrukerPolicyInput(
                navAnsattAzureId = navAnsatt.azureId,
                tilgangType = tilgangType,
                norskIdent = identitetsnummer
            )
        )
        val tilgang = navAnsattTilgang.getOrDefault {
            throw BrukerHarIkkeTilgangException("Kunne ikke finne tilgang for ansatt")
        }

        if (tilgang.isDeny) {
            logger.info("NAV-ansatt har ikke $tilgangType til bruker")
        } else {
            auditLogger.audit(
                identitetsnummer,
                navAnsatt,
                tilgangType,
                "NAV ansatt har benyttet $tilgangType tilgang til informasjon om bruker"
            )
        }
    }
}

data class NavAnsatt(val azureId: UUID, val navIdent: String)
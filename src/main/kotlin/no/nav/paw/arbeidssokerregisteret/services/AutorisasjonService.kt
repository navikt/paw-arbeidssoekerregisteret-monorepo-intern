package no.nav.paw.arbeidssokerregisteret.services

import no.nav.paw.arbeidssokerregisteret.domain.Foedselsnummer
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.utils.logger
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.poao_tilgang.client.TilgangType

class AutorisasjonService(
    private val poaoTilgangHttpClient: PoaoTilgangHttpClient
) {
    fun verifiserVeilederTilgangTilBruker(navAnsatt: NavAnsatt, foedselsnummer: Foedselsnummer): Boolean {
        logger.info("NAV-ansatt forsøker å hente informasjon om bruker: $foedselsnummer")

        val harNavAnsattTilgang = poaoTilgangHttpClient.evaluatePolicy(
            NavAnsattTilgangTilEksternBrukerPolicyInput(
                navAnsatt.azureId,
                TilgangType.SKRIVE,
                foedselsnummer.verdi
            )
        ).getOrThrow()
            .isPermit

        if (!harNavAnsattTilgang) {
            logger.warn("NAV-ansatt har ikke tilgang til bruker: $foedselsnummer (v/poao-tilgang)")
        } else {
            logger.info("NAV-ansatt har hentet informasjon om bruker: $foedselsnummer")
        }

        return harNavAnsattTilgang
    }
}

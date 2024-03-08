package no.nav.paw.arbeidssokerregisteret.services

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.utils.auditLogMessage
import no.nav.paw.arbeidssokerregisteret.utils.autitLogger
import no.nav.paw.arbeidssokerregisteret.utils.logger
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.poao_tilgang.client.TilgangType

class AutorisasjonService(
    private val poaoTilgangHttpClient: PoaoTilgangHttpClient
) {
    fun verifiserVeilederTilgangTilBruker(navAnsatt: NavAnsatt, identitetsnummer: Identitetsnummer): Boolean {
        logger.info("NAV-ansatt forsøker å hente informasjon om bruker: $identitetsnummer")

        val harNavAnsattTilgang = poaoTilgangHttpClient.evaluatePolicy(
            NavAnsattTilgangTilEksternBrukerPolicyInput(
                navAnsatt.azureId,
                TilgangType.SKRIVE,
                identitetsnummer.verdi
            )
        ).getOrThrow()
            .isPermit

        if (!harNavAnsattTilgang) {
            logger.warn("NAV-ansatt har ikke tilgang til bruker: $identitetsnummer (v/poao-tilgang)")
        } else {
            autitLogger.info(
                auditLogMessage(identitetsnummer, navAnsatt, "NAV-ansatt har registrert informasjon om bruker")
            )
            logger.info("NAV-ansatt har hentet informasjon om bruker: $identitetsnummer")
        }

        return harNavAnsattTilgang
    }
}

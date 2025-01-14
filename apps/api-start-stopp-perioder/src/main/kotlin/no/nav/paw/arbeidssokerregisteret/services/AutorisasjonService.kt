package no.nav.paw.arbeidssokerregisteret.services

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.utils.auditLogMessage
import no.nav.paw.arbeidssokerregisteret.utils.autitLogger
import no.nav.paw.arbeidssokerregisteret.utils.logger
import no.nav.paw.error.model.getOrThrow
import no.nav.paw.model.NavIdent
import no.nav.paw.tilgangskontroll.client.Tilgang
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte

class AutorisasjonService(
    private val tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte
) {
    suspend fun verifiserVeilederTilgangTilBruker(navAnsatt: NavAnsatt, identitetsnummer: Identitetsnummer): Boolean {
        logger.info("NAV-ansatt forsøker å hente informasjon om bruker: $identitetsnummer")

        val harNavAnsattTilgang = tilgangsTjenesteForAnsatte.harAnsattTilgangTilPerson(
            navIdent = NavIdent(navAnsatt.ident),
            identitetsnummer = no.nav.paw.model.Identitetsnummer(identitetsnummer.verdi),
            tilgang = Tilgang.SKRIVE
        ).getOrThrow()

        if (!harNavAnsattTilgang) {
            logger.warn("NAV-ansatt har ikke tilgang til bruker: $identitetsnummer (v/poao-tilgang)")
        } else {
            autitLogger.info(
                auditLogMessage(identitetsnummer, navAnsatt, "NAV-ansatt har registrert informasjon om bruker")
            )
            logger.info("NAV-ansatt har hentet informasjon om bruker: $identitetsnummer") // er dette sånn superlogging som ikke havner rett i elastic?
        }

        return harNavAnsattTilgang
    }
}

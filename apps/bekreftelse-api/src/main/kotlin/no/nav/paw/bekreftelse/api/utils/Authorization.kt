package no.nav.paw.bekreftelse.api.utils

import no.nav.paw.security.authentication.model.Bruker
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.Anonym
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.asIdentitetsnummer
import no.nav.paw.security.authorization.exception.IngenTilgangException

fun Bruker<*>.hentSluttbrukerIdentitet(): Identitetsnummer {
    return when (this) {
        is Sluttbruker -> ident
        else -> throw IngenTilgangException("Endepunkt kan kun benyttes av sluttbruker")
    }
}

fun Bruker<*>.hentSluttbrukerIdentitet(identitetsnummer: String?): Identitetsnummer {
    return when (this) {
        is Sluttbruker -> {
            identitetsnummer?.let { if (ident.verdi != it) throw IngenTilgangException("Bruker har ikke tilgang til sluttbrukers informasjon") }
            ident
        }

        is NavAnsatt -> identitetsnummer?.asIdentitetsnummer()
            ?: throw IngenTilgangException("Veileder må sende med identitetsnummer for sluttbruker")

        is Anonym -> identitetsnummer?.asIdentitetsnummer()
            ?: throw IngenTilgangException("Anonym må sende med identitetsnummer for sluttbruker")

        else -> throw IngenTilgangException("Endepunkt kan ikke benyttes av ukjent brukergruppe")
    }
}
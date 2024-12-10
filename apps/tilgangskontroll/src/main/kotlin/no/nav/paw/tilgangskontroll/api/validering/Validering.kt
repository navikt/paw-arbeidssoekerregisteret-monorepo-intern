package no.nav.paw.tilgangskontroll.api.validering

import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollRequestV1
import no.nav.paw.tilgangskontroll.vo.Identitetsnummer
import no.nav.paw.tilgangskontroll.vo.EntraId
import no.nav.paw.tilgangskontroll.vo.Tilgang

fun TilgangskontrollRequestV1?.valider(): ValidertTilgangskontrollRequest {
    requireNotNull(this) { "Request er 'null'" }
    require(identitetsnummer.matches(Regex("\\d{11}"))) { "Ugyldig identitetsnummer" }
    return ValidertTilgangskontrollRequest(
        person = Identitetsnummer(identitetsnummer),
        navAnsatt = EntraId(navAnsattId),
        tilgang = Tilgang.valueOf(tilgang.name)
    )
}

@JvmRecord
data class ValidertTilgangskontrollRequest(
    val person: Identitetsnummer,
    val navAnsatt: EntraId,
    val tilgang: Tilgang
)
package no.nav.paw.tilgangskontroll.api.validering

import io.ktor.http.HttpStatusCode
import no.nav.paw.error.exception.ServerResponseException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.NavIdent
import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollRequestV1
import no.nav.paw.tilgangskontroll.vo.Tilgang

fun TilgangskontrollRequestV1?.valider(): ValidertTilgangskontrollRequest {
    try {
        requireNotNull(this) { "Request er 'null'" }
        require(identitetsnummer.matches(Regex("\\d{11}"))) { "Ugyldig identitetsnummer" }
    } catch (ex: IllegalArgumentException) {
        throw ServerResponseException(
            status = HttpStatusCode.BadRequest,
            type = ErrorType.domain("http").error("ugyldig-foresporsel").build(),
            message = ex.message ?: "Ugyldig forespørsel",
            cause = ex
        )
    }
    return ValidertTilgangskontrollRequest(
        person = Identitetsnummer(identitetsnummer),
        navAnsatt = NavIdent(navAnsattId),
        tilgang = Tilgang.valueOf(tilgang.name)
    )
}

@JvmRecord
data class ValidertTilgangskontrollRequest(
    val person: Identitetsnummer,
    val navAnsatt: NavIdent,
    val tilgang: Tilgang
)
package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.http.*

abstract class ArbeidssoekerException(
    val status: HttpStatusCode,
    description: String? = null,
    val errorCode: ErrorCode,
    causedBy: Throwable? = null
) : Exception("Request failed with status: $status. Description: $description. ErrorCode: $errorCode", causedBy)

enum class ErrorCode {
    FEIL,
    UVENTET_FEIL_MOT_EKSTERNE_TJENESTER,
    UGYLDIG_JSON,

}

package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.uri
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.ProblemDetailsBuilder

fun customExceptionResolver(): (Throwable, ApplicationRequest) -> ProblemDetails? = { throwable, request ->
    when (throwable) {
        is IngenHendelserFunnet -> {
            ProblemDetailsBuilder.builder()
                .type(ErrorType.domain("http").error("ingen-hendelser-funnet").build())
                .status(HttpStatusCode.NotFound)
                .detail(throwable.message ?: "Fant ingen hendelser for bruker")
                .instance(request.uri)
                .build()
        }
        is FantIkkeIdentitetsnummer -> {
            ProblemDetailsBuilder.builder()
                .type(ErrorType.domain("http").error("fant-ikke-identitetsnummer").build())
                .status(HttpStatusCode.NotFound)
                .detail(throwable.message ?: "Fant ikke identitetsnummer")
                .instance(request.uri)
                .build()
        }
        is UgyldigIdentFormat -> {
            ProblemDetailsBuilder.builder()
                .type(ErrorType.domain("http").error("ugyldig-identformat").build())
                .status(HttpStatusCode.BadRequest)
                .detail(throwable.message ?: "Kunne ikke parse til ident eller periodeid")
                .instance(request.uri)
                .build()
        }

        else -> null
    }
}
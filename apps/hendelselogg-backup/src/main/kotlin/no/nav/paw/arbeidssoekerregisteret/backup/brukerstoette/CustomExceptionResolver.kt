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

        else -> null
    }
}
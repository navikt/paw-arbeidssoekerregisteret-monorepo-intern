package no.nav.paw.bekreftelse.api.exception

import no.nav.paw.error.exception.ClientResponseException
import no.nav.paw.error.model.ProblemDetails

class ProblemDetailsException(problem: ProblemDetails) : ClientResponseException(
    problem.status, problem.code, problem.detail, null
)
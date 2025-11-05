package no.nav.paw.error.exception

import no.nav.paw.error.model.ProblemDetails

open class ProblemDetailsException(problemDetails: ProblemDetails) : ErrorTypeAwareException(
    type = problemDetails.type,
    message = problemDetails.title
)

package no.nav.paw.error.exception

import no.nav.paw.error.model.ProblemDetails

open class ProblemDetailsException(val details: ProblemDetails): ErrorTypeAwareException(
    type = details.type,
    message = details.title
)

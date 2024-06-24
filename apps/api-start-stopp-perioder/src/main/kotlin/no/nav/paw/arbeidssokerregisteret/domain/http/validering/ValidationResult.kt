package no.nav.paw.arbeidssokerregisteret.domain.http.validering

data class ValidationErrorResult(
    val fields: Set<String>,
    val message: String
)

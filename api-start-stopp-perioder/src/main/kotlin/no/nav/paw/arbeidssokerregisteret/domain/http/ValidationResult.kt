package no.nav.paw.arbeidssokerregisteret.domain.http

sealed interface ValidationResult
data object ValidationResultOk: ValidationResult

data class ValidationErrorResult(
    val fields: Set<String>,
    val message: String
): ValidationResult

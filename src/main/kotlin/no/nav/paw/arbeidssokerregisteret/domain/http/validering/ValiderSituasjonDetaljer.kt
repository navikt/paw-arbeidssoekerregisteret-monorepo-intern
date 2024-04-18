package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Detaljer
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JobbsituasjonMedDetaljer
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationErrorResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResultOk

val validationRules =
    listOf(
        Detaljer::prosent to ::isValidPercentage,
        Detaljer::stillingStyrk08 to ::validateStyrk08,
        Detaljer::stilling to ::isNotEmptyString
    )

fun validerDetaljer(situasjonMedDetaljer: JobbsituasjonMedDetaljer): ValidationResult =
    validationRules
        .mapNotNull { (property, rule) ->
            property(situasjonMedDetaljer.detaljer)
                ?.let { rule(property.name, it) }
        }.filterIsInstance<ValidationErrorResult>()
        .firstOrNull() ?: ValidationResultOk


fun isValidPercentage(key: String, percent: String): ValidationResult =
    try {
        val verdi = percent.toDouble()
        if (verdi > 0.0 && verdi <= 100.0) {
            ValidationResultOk
        } else {
            ValidationErrorResult(setOf(key), "Prosent må være et tall mellom 0 og 100. Var $percent")
        }
    } catch (e: Exception) {
        ValidationErrorResult(setOf(key), "Prosent må være et tall mellom 0 og 100. Var $percent")
    }

fun isNotEmptyString(key: String, string: String): ValidationResult =
    if (string.isNotBlank()) {
        ValidationResultOk
    } else {
        ValidationErrorResult(setOf(key), "$key kan ikke være tom")
    }

fun validateStyrk08(key: String, styrk: String): ValidationResult =
    if (Regex("[0-9]{1,4}").matches(styrk)) {
        ValidationResultOk
    } else {
        ValidationErrorResult(setOf(key), "Styrk08 må være 1-4 siffer. Styrk08 var $styrk")
    }

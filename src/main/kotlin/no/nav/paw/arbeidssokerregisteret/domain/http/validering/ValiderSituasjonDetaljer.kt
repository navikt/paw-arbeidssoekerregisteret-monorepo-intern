package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationErrorResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResultOk
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonMedDetaljer
import java.time.LocalDate

fun validerDetaljer(detaljer: JobbsituasjonMedDetaljer): ValidationResult =
    detaljer.detaljer.mapNotNull {
        validation[it.key]?.let { function -> function(it.key, it.value) }
    }.filterIsInstance<ValidationErrorResult>().firstOrNull() ?: ValidationResultOk


val validation: Map<String, (String, String) -> ValidationResult> = mapOf(
    STILLING_STYRK08 to ::validateStyrk08,
    STILLING to ::isNotBlank,
    GJELDER_FRA_DATO to ::validIso8601Date,
    GJELDER_TIL_DATO to ::validIso8601Date,
    SISTE_ARBEIDSDAG to ::validIso8601Date,
    SISTE_DAG_MED_LOENN to ::validIso8601Date,
    PROSENT to ::isValidPercentage
)

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

fun validIso8601Date(key: String, date: String): ValidationResult =
    try {
        LocalDate.parse(date)
        ValidationResultOk
    } catch (e: Exception) {
        ValidationErrorResult(setOf(key), "Dato må være på formatet iso8601. Dato var $date")
    }

fun isNotBlank(key: String, string: String): ValidationResult =
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


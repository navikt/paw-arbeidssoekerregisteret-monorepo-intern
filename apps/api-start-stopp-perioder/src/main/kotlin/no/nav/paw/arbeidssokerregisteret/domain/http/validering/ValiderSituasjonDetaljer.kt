package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Detaljer
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JobbsituasjonMedDetaljer

val validationRules =
    listOf(
        Detaljer::prosent to ::isValidPercentage,
        Detaljer::stillingStyrk08 to ::validateStyrk08,
        Detaljer::stilling to ::isNotEmptyString,
        Detaljer::stilling to ::validerFritekstFelt
    )

val ingenDetaljer: Detaljer = Detaljer(null, null, null)

val fritekstRegex = Regex(("^[^{}<>;]+$"))

fun validerDetaljer(situasjonMedDetaljer: JobbsituasjonMedDetaljer): Either<ValidationErrorResult, Unit> =
    validationRules
        .mapNotNull { (property, rule) ->
            property(situasjonMedDetaljer.detaljer ?: ingenDetaljer)
                ?.let { rule(property.name, it) }
        }.filterIsInstance<ValidationErrorResult>()
        .firstOrNull()?.left() ?: Unit.right()


fun isValidPercentage(key: String, percent: String): Either<ValidationErrorResult, Unit> =
    try {
        val verdi = percent.toDouble()
        if (verdi > 0.0 && verdi <= 100.0) {
            Unit.right()
        } else {
            ValidationErrorResult(setOf(key), "Prosent må være et tall mellom 0 og 100. Var $percent").left()
        }
    } catch (e: Exception) {
        ValidationErrorResult(setOf(key), "Prosent må være et tall mellom 0 og 100. Var $percent").left()
    }

fun isNotEmptyString(key: String, string: String): Either<ValidationErrorResult, Unit> =
    if (string.isNotBlank()) {
        Unit.right()
    } else {
        ValidationErrorResult(setOf(key), "$key kan ikke være tom").left()
    }

fun validateStyrk08(key: String, styrk: String): Either<ValidationErrorResult, Unit> =
    if (Regex("[0-9]{1,4}").matches(styrk)) {
        Unit.right()
    } else {
        ValidationErrorResult(setOf(key), "Styrk08 må være 1-4 siffer. Styrk08 var $styrk").left()
    }

fun validerFritekstFelt(key: String, stilling: String): Either<ValidationErrorResult, Unit> =
    if (fritekstRegex.matches(stilling)) {
        Unit.right()
    } else {
        ValidationErrorResult(
            fields = setOf(key),
            message = "$key kan bare inneholde bokstaver, tall og vanlige spesialtegn. $key var $stilling"
        ).left()
    }

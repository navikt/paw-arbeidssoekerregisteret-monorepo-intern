package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse.*
import java.time.LocalDate

data class OpplysningerRequest(
    val identitetsnummer: String,
    val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerRequest
) {
    fun getId(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()
}

data class OpplysningerOmArbeidssoekerRequest(
    val utdanning: Utdanning,
    val helse: Helse,
    val jobbsituasjon: Jobbsituasjon,
    val annet: Annet
)

/*
0 - Ingen utdanning og førskoleutdanning
1 - Barneskoleutdanning
2 - Ungdomsskoleutdanning
3 - Videregående, grunnutdanning
4 - Videregående, avsluttende utdanning
5 - Påbygging til videregående utdanning
6 - Universitets- og høgskoleutdanning, lavere nivå
7 - Universitets- og høgskoleutdanning, høyere nivå
8 - Forskerutdanning
9 - Uoppgitt
 */
fun validerUtdanning(utdanning: Utdanning): ValidationResult {
    val nus = utdanning.nus
    if (!Regex("[0-9]").matches(nus)) {
        return ValidationErrorResult(setOf("nus"), "Nus må være 0-9. Nus var $nus")
    }
    if (nus in setOf("0", "1", "2", "9") && utdanning.bestaatt != null) {
        return ValidationErrorResult(setOf("nus", "bestaatt"), "Forventer ikke svar på bestått ved nus $nus")
    }
    if (nus in setOf("0", "1", "2", "9") && utdanning.godkjent != null) {
        return ValidationErrorResult(setOf("nus", "godkjent"), "Forventer ikke svar på godkjent ved nus $nus")
    }
    if (nus in setOf("3", "4", "5", "6", "7", "8") && utdanning.bestaatt == null) {
        return ValidationErrorResult(setOf("nus", "bestatt"), "Forventer svar på bestatt ved nus $nus")
    }
    if (nus in setOf("3", "4", "5", "6", "7", "8") && utdanning.godkjent == null) {
        return ValidationErrorResult(setOf("nus", "godkjent"), "Forventer svar på godkjent ved nus $nus")
    }
    return ValidationResultOk
}

/*
UKJENT_VERDI
UDEFINERT
HAR_SAGT_OPP
HAR_BLITT_SAGT_OPP
ER_PERMITTERT
ALDRI_HATT_JOBB
IKKE_VAERT_I_JOBB_SISTE_2_AAR
AKKURAT_FULLFORT_UTDANNING
VIL_BYTTE_JOBB
USIKKER_JOBBSITUASJON
MIDLERTIDIG_JOBB
DELTIDSJOBB_VIL_MER
NY_JOBB
KONKURS
ANNET
 */
fun validerJobbsituasjon(jobbsituasjon: Jobbsituasjon): ValidationResult {
    val beskrivelser = jobbsituasjon.beskrivelser.map { it.beskrivelse }
    val requiresStyrk08 = setOf(
        ER_PERMITTERT,
        HAR_SAGT_OPP,
        HAR_BLITT_SAGT_OPP,
        DELTIDSJOBB_VIL_MER,
        KONKURS,
        MIDLERTIDIG_JOBB,
        USIKKER_JOBBSITUASJON,
        NY_JOBB
    )
    val missingStyrk08 = jobbsituasjon.beskrivelser
        .filter { it.beskrivelse in requiresStyrk08 }
        .filterNot { it.detaljer.contains(STILLING_STYRK08) }
    if (missingStyrk08.isNotEmpty()) {
        return ValidationErrorResult(
            setOf("jobbsituasjon.beskrivelser"),
            "Mangler '$STILLING_STYRK08' for ${missingStyrk08.joinToString { it.beskrivelse.name }}"
        )
    }
    return ValidationResultOk
}

val validation: Map<String, (String, String) -> ValidationResult> = mapOf(
    STILLING_STYRK08 to ::validateStyrk08,
    STILLING to ::isNotBlank,
    GJELDER_FRA_DATO to ::validIso8601Date,
    GJELDER_TIL_DATO to ::validIso8601Date,
    SISTE_ARBEIDSDAG to ::validIso8601Date,
    SISTE_DAG_MED_LOENN to ::validIso8601Date,
    PROSENT to ::isValidPercentage
    )

fun isValidPercentage(key: String, percent: String): ValidationResult=
    try {
        val verdi = percent.toDouble()
        if (verdi > 0.0 && verdi <= 100.0){
            ValidationResultOk
        } else {
            ValidationErrorResult(setOf(key), "Prosent må være et tall mellom 0 og 100. Var $percent")}
    } catch (e: Exception){
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
        ValidationErrorResult(setOf("stilling"), "Stilling kan ikke være tom")
    }

fun validateStyrk08(key: String, styrk: String): ValidationResult =
    if (Regex("[0-9]{1,4}").matches(styrk)) {
        ValidationResultOk
    } else {
        ValidationErrorResult(setOf(key), "Styrk08 må være 1-4 siffer. Styrk08 var $styrk")
    }


fun validerBeskrivelser(beskrivelser: Set<JobbsituasjonBeskrivelse>): ValidationResult {
    val notValidWithAldriHattJobb = setOf(
        ER_PERMITTERT,
        DELTIDSJOBB_VIL_MER,
        MIDLERTIDIG_JOBB,
        KONKURS,
        USIKKER_JOBBSITUASJON
    )
    if (beskrivelser.contains(ALDRI_HATT_JOBB) &&
        beskrivelser.any { it in notValidWithAldriHattJobb }
    ) {
        return ValidationErrorResult(
            setOf("beskrivelser"),
            "Kan ikke ha beskrivelsene $notValidWithAldriHattJobb samtidig med $ALDRI_HATT_JOBB"
        )
    }
    if (beskrivelser.contains(ER_PERMITTERT) && beskrivelser.contains(IKKE_VAERT_I_JOBB_SISTE_2_AAR)) {
        return ValidationErrorResult(setOf("beskrivelser"), "Kan ikke være permittert og ikke vært i jobb siste 2 år samtidig")
    }
    TODO()
}

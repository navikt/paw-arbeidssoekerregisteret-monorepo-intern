package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Utdanning
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationErrorResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResultOk

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
    val forventerBestaattOgGodkjent = setOf("3", "4", "5", "6", "7", "8")
    if (!Regex("[0-9]").matches(nus)) {
        return ValidationErrorResult(setOf("nus"), "Nus må være 0-9. Nus var $nus")
    }
    if (nus !in forventerBestaattOgGodkjent) {
        if (utdanning.bestaatt != null || utdanning.godkjent != null) {
            return ValidationErrorResult(setOf("nus", "bestaatt", "godkjent"), "Forventer ikke svar på bestått eller godkjent ved nus $nus")
        }
    } else {
        if (utdanning.bestaatt == null || utdanning.godkjent == null) {
            return ValidationErrorResult(setOf("nus", "bestatt", "godkjent"), "Forventer svar på bestatt og godkjent ved nus $nus")
        }
    }
    return ValidationResultOk
}

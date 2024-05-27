package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JobbsituasjonMedDetaljer
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JobbsituasjonMedDetaljer.Beskrivelse.*
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationErrorResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResultOk

fun validerBeskrivelser(beskrivelser: List<JobbsituasjonMedDetaljer.Beskrivelse>): ValidationResult {
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
    return ValidationResultOk
}

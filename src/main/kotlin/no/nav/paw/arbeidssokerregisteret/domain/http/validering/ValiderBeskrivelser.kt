package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationErrorResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResultOk
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse

fun validerBeskrivelser(beskrivelser: List<JobbsituasjonBeskrivelse>): ValidationResult {
    val notValidWithAldriHattJobb = setOf(
        JobbsituasjonBeskrivelse.ER_PERMITTERT,
        JobbsituasjonBeskrivelse.DELTIDSJOBB_VIL_MER,
        JobbsituasjonBeskrivelse.MIDLERTIDIG_JOBB,
        JobbsituasjonBeskrivelse.KONKURS,
        JobbsituasjonBeskrivelse.USIKKER_JOBBSITUASJON
    )
    if (beskrivelser.contains(JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB) &&
        beskrivelser.any { it in notValidWithAldriHattJobb }
    ) {
        return ValidationErrorResult(
            setOf("beskrivelser"),
            "Kan ikke ha beskrivelsene $notValidWithAldriHattJobb samtidig med ${JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB}"
        )
    }
    if (beskrivelser.contains(JobbsituasjonBeskrivelse.ER_PERMITTERT) && beskrivelser.contains(JobbsituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR)) {
        return ValidationErrorResult(setOf("beskrivelser"), "Kan ikke være permittert og ikke vært i jobb siste 2 år samtidig")
    }
    return ValidationResultOk
}

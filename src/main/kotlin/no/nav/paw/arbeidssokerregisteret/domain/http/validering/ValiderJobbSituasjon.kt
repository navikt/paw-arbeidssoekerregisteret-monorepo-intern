package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationErrorResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResult
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse


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
    val requiresStyrk08 = setOf(
        JobbsituasjonBeskrivelse.ER_PERMITTERT,
        JobbsituasjonBeskrivelse.HAR_SAGT_OPP,
        JobbsituasjonBeskrivelse.HAR_BLITT_SAGT_OPP,
        JobbsituasjonBeskrivelse.DELTIDSJOBB_VIL_MER,
        JobbsituasjonBeskrivelse.KONKURS,
        JobbsituasjonBeskrivelse.MIDLERTIDIG_JOBB,
        JobbsituasjonBeskrivelse.USIKKER_JOBBSITUASJON,
        JobbsituasjonBeskrivelse.NY_JOBB
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

    val detaljerError = jobbsituasjon.beskrivelser
        .map(::validerDetaljer)
        .filterIsInstance<ValidationErrorResult>()
    if (detaljerError.isNotEmpty()){
        return  detaljerError.first()
    }

    return validerBeskrivelser(jobbsituasjon.beskrivelser.map { it.beskrivelse })
}

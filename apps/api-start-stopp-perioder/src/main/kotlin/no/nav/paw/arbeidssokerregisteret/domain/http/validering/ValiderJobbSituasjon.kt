package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import arrow.core.Either
import arrow.core.left
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Jobbsituasjon
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JobbsituasjonMedDetaljer.Beskrivelse.*
import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08

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

fun validerJobbsituasjon(jobbsituasjon: Jobbsituasjon): Either<ValidationErrorResult, Unit> {
    val requiresStyrk08 = setOf(
        ER_PERMITTERT,
        HAR_SAGT_OPP,
        HAR_BLITT_SAGT_OPP,
        DELTIDSJOBB_VIL_MER,
        KONKURS,
        MIDLERTIDIG_JOBB,
        NY_JOBB
    )
    val missingStyrk08 = jobbsituasjon.beskrivelser
        .filter { it.beskrivelse in requiresStyrk08 }
        .filterNot { it.detaljer?.stillingStyrk08 != null }
    if (missingStyrk08.isNotEmpty()) {
        return ValidationErrorResult(
            setOf("jobbsituasjon.beskrivelser"),
            "Mangler '$STILLING_STYRK08' for ${missingStyrk08.joinToString { it.beskrivelse.name }}"
        ).left()
    }

    val detaljerError = jobbsituasjon.beskrivelser
        .map(::validerDetaljer)
        .filterIsInstance<ValidationErrorResult>()
    if (detaljerError.isNotEmpty()){
        return  detaljerError.first().left()
    }

    return validerBeskrivelser(jobbsituasjon.beskrivelser.map { it.beskrivelse })
}

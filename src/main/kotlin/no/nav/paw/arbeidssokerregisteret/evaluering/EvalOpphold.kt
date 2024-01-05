package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentperson.Opphold
import java.time.LocalDate


fun evalOppholdstillatelse(oppholdtillatelse: Opphold?): Evaluation {
    val tilDato = oppholdtillatelse?.oppholdTil?.let(LocalDate::parse)
    val utloept = tilDato?.isBefore(LocalDate.now()) ?: false
    return if (utloept) {
        Evaluation.OPPHOLDSTILATELSE_UTGAATT
    } else
        when (oppholdtillatelse?.type) {
            Oppholdstillatelse.MIDLERTIDIG -> Evaluation.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.PERMANENT -> Evaluation.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.OPPLYSNING_MANGLER -> Evaluation.BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE
            Oppholdstillatelse.__UNKNOWN_VALUE -> Evaluation.UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE
            null -> Evaluation.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE
        }
}

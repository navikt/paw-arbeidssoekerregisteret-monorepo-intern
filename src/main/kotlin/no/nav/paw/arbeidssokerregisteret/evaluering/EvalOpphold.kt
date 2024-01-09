package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentperson.Opphold
import java.time.LocalDate


fun evalOppholdstillatelse(oppholdtillatelse: Opphold?): Attributter {
    val tilDato = oppholdtillatelse?.oppholdTil?.let(LocalDate::parse)
    val utloept = tilDato?.isBefore(LocalDate.now()) ?: false
    return if (utloept) {
        Attributter.OPPHOLDSTILATELSE_UTGAATT
    } else
        when (oppholdtillatelse?.type) {
            Oppholdstillatelse.MIDLERTIDIG -> Attributter.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.PERMANENT -> Attributter.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.OPPLYSNING_MANGLER -> Attributter.BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE
            Oppholdstillatelse.__UNKNOWN_VALUE -> Attributter.UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE
            null -> Attributter.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE
        }
}

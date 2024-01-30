package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentperson.Opphold
import java.time.LocalDate


fun evalOppholdstillatelse(oppholdtillatelse: Opphold?): Attributt {
    val tilDato = oppholdtillatelse?.oppholdTil?.let(LocalDate::parse)
    val utloept = tilDato?.isBefore(LocalDate.now()) ?: false
    return if (utloept) {
        Attributt.OPPHOLDSTILATELSE_UTGAATT
    } else
        when (oppholdtillatelse?.type) {
            Oppholdstillatelse.MIDLERTIDIG -> Attributt.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.PERMANENT -> Attributt.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.OPPLYSNING_MANGLER -> Attributt.BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE
            Oppholdstillatelse.__UNKNOWN_VALUE -> Attributt.UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE
            null -> Attributt.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE
        }
}

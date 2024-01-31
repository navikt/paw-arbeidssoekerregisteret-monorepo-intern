package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Fakta
import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentperson.Opphold
import java.time.LocalDate


fun evalOppholdstillatelse(oppholdtillatelse: Opphold?): Fakta {
    val tilDato = oppholdtillatelse?.oppholdTil?.let(LocalDate::parse)
    val utloept = tilDato?.isBefore(LocalDate.now()) ?: false
    return if (utloept) {
        Fakta.OPPHOLDSTILATELSE_UTGAATT
    } else
        when (oppholdtillatelse?.type) {
            Oppholdstillatelse.MIDLERTIDIG -> Fakta.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.PERMANENT -> Fakta.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.OPPLYSNING_MANGLER -> Fakta.BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE
            Oppholdstillatelse.__UNKNOWN_VALUE -> Fakta.UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE
            null -> Fakta.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE
        }
}

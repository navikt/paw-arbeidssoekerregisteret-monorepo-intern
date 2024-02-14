package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentperson.Opphold
import java.time.LocalDate


fun oppholdstillatelseFakta(oppholdtillatelse: Opphold?): Opplysning {
    val tilDato = oppholdtillatelse?.oppholdTil?.let(LocalDate::parse)
    val utloept = tilDato?.isBefore(LocalDate.now()) ?: false
    return if (utloept) {
        Opplysning.OPPHOLDSTILATELSE_UTGAATT
    } else
        when (oppholdtillatelse?.type) {
            Oppholdstillatelse.MIDLERTIDIG -> Opplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.PERMANENT -> Opplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE
            Oppholdstillatelse.OPPLYSNING_MANGLER -> Opplysning.BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE
            Oppholdstillatelse.__UNKNOWN_VALUE -> Opplysning.UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE
            null -> Opplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE
        }
}

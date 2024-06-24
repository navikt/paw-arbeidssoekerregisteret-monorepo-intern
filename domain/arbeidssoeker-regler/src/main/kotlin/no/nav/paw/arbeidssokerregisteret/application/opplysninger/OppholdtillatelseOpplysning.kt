package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentperson.Opphold
import java.time.LocalDate
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*


fun oppholdstillatelseOpplysning(oppholdtillatelse: Opphold?): Opplysning {
    val tilDato = oppholdtillatelse?.oppholdTil?.let(LocalDate::parse)
    val utloept = tilDato?.isBefore(LocalDate.now()) ?: false
    return if (utloept) {
        OppholdstillatelseUtgaaatt
    } else
        when (oppholdtillatelse?.type) {
            Oppholdstillatelse.MIDLERTIDIG -> HarGyldigOppholdstillatelse
            Oppholdstillatelse.PERMANENT -> HarGyldigOppholdstillatelse
            Oppholdstillatelse.OPPLYSNING_MANGLER -> BarnFoedtINorgeUtenOppholdstillatelse
            Oppholdstillatelse.__UNKNOWN_VALUE -> UkjentStatusForOppholdstillatelse
            null -> IngenInformasjonOmOppholdstillatelse
        }
}

package no.nav.paw.arbeidssokerregisteret.domain

import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentopphold.Opphold
import java.time.LocalDate

fun List<Opphold>?.harOppholdstillatelse(): Boolean = this
    ?.filterNot { setOf(Oppholdstillatelse.OPPLYSNING_MANGLER, Oppholdstillatelse.__UNKNOWN_VALUE).contains(it.type) }
    ?.any { it.oppholdTil == null || LocalDate.parse(it.oppholdTil.toString()).isAfter(LocalDate.now()) }
    ?: false

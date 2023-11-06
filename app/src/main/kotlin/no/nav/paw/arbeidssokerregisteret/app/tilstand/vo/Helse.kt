package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.Helse as ApiHelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Helse as InternApiHelse

data class Helse(
    val helsetilstandHindrerArbeid: JaNeiVetIkke,
)

fun helse(helse: InternApiHelse): Helse =
    Helse(
        helsetilstandHindrerArbeid = jaNeiVetIkke(helse.helsetilstandHindrerArbeid)
    )

fun Helse.api(): ApiHelse =
    ApiHelse(
        helsetilstandHindrerArbeid.api()
    )
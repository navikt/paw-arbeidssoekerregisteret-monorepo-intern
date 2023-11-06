package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.Utdanning as ApiUtdanning
import no.nav.paw.arbeidssokerregisteret.intern.v1.Utdanning as InternUtdanning

data class Utdanning(
    val utdanningsnivaa: Utdanningsnivaa,
    val bestaatt: JaNeiVetIkke,
    val godkjent: JaNeiVetIkke,
)

fun utdanning(utdanning: InternUtdanning): Utdanning =
    Utdanning(
        utdanningsnivaa = utdaningsnivaa(utdanning.lengde),
        bestaatt = jaNeiVetIkke(utdanning.bestaatt),
        godkjent = jaNeiVetIkke(utdanning.godkjent),
    )

fun Utdanning.api(): ApiUtdanning =
    ApiUtdanning(
        utdanningsnivaa.api(),
        bestaatt.api(),
        godkjent.api(),
    )
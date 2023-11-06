package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker as ApiBruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.Bruker as InternApiBruker

data class Bruker(
    val id: String,
    val type: BrukerType
)

fun bruker(bruker: InternApiBruker): Bruker =
    Bruker(
        id = bruker.id,
        type = brukerType(bruker.type)
    )

fun Bruker.api(): ApiBruker = ApiBruker(type.api(), id)
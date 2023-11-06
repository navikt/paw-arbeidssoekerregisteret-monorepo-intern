package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import java.time.Instant
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as ApiMetadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.Metadata as InternApiMetadata

data class Metadata(
    val tidspunkt: Instant,
    val utfoertAv: Bruker,
    val kilde: String,
    val aarsak: String
)

fun metadata(metadata: InternApiMetadata): Metadata =
    Metadata(
        tidspunkt = metadata.tidspunkt,
        utfoertAv = bruker(metadata.utfoertAv),
        kilde = metadata.kilde,
        aarsak = metadata.aarsak
    )

fun Metadata.api(): ApiMetadata =
    ApiMetadata(
        tidspunkt,
        utfoertAv.api(),
        kilde,
        aarsak
    )
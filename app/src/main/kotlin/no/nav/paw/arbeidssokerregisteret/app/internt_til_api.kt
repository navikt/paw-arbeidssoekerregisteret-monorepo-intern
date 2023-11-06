package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.Periode

fun Periode?.tilApiPeriode(): no.nav.paw.arbeidssokerregisteret.api.v1.Periode? {
    return this?.let {
        no.nav.paw.arbeidssokerregisteret.api.v1.Periode(
            id,
            identitetsnummer,
            startet.tilApiMetadata(),
            avsluttet?.tilApiMetadata()
        )
    }
}

fun Metadata.tilApiMetadata(): no.nav.paw.arbeidssokerregisteret.api.v1.Metadata {
    return no.nav.paw.arbeidssokerregisteret.api.v1.Metadata(
        tidspunkt,
        utfoertAv.tilApiBruker(),
        kilde,
        aarsak
    )
}

fun no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.Bruker.tilApiBruker(): Bruker {
    return Bruker(
        type.tilApiBrukerType(),
        id
    )
}

fun BrukerType.tilApiBrukerType(): no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType {
    return when (this) {
        BrukerType.VEILEDER -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.VEILEDER
        BrukerType.SYSTEM -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SYSTEM
        BrukerType.SLUTTBRUKER -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SLUTTBRUKER
        BrukerType.UKJENT_VERDI -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.UKJENT_VERDI
        BrukerType.UDEFINERT -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.UDEFINERT
    }
}
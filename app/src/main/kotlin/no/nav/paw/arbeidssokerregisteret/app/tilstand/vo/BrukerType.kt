package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType as ApiBrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.BrukerType as InternApiBrukerType

enum class BrukerType {
    UDEFINERT,
    UKJENT_VERDI,
    SYSTEM,
    SLUTTBRUKER,
    VEILEDER
}

fun brukerType(brukerType: InternApiBrukerType): BrukerType =
    when (brukerType) {
        InternApiBrukerType.VEILEDER -> BrukerType.VEILEDER
        InternApiBrukerType.SYSTEM -> BrukerType.SYSTEM
        InternApiBrukerType.SLUTTBRUKER -> BrukerType.SLUTTBRUKER
        InternApiBrukerType.UKJENT_VERDI -> BrukerType.UKJENT_VERDI
        InternApiBrukerType.UDEFINERT -> BrukerType.UDEFINERT
    }

fun BrukerType.api(): ApiBrukerType =
    when (this) {
        BrukerType.VEILEDER -> ApiBrukerType.VEILEDER
        BrukerType.SYSTEM -> ApiBrukerType.SYSTEM
        BrukerType.SLUTTBRUKER -> ApiBrukerType.SLUTTBRUKER
        BrukerType.UKJENT_VERDI -> ApiBrukerType.UKJENT_VERDI
        BrukerType.UDEFINERT -> ApiBrukerType.UDEFINERT
    }
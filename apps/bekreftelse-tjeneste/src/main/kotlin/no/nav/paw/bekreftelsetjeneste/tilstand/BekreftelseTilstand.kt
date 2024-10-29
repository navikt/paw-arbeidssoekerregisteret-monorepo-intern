package no.nav.paw.bekreftelsetjeneste.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode

const val MAKS_ANTALL_HISTRISKE_BEKREFTELSER = 20

@JvmRecord
data class BekreftelseTilstand(
    val periode: PeriodeInfo,
    val bekreftelser: List<Bekreftelse>
)

fun opprettBekreftelseTilstand(
    id: Long,
    key: Long,
    periode: Periode,
): BekreftelseTilstand =
    BekreftelseTilstand(
        periode = PeriodeInfo(
            periodeId = periode.id,
            identitetsnummer = periode.identitetsnummer,
            arbeidsoekerId = id,
            recordKey = key,
            startet = periode.startet.tidspunkt,
            avsluttet = periode.avsluttet?.tidspunkt
        ),
        bekreftelser = emptyList()
    )

fun BekreftelseTilstand.oppdaterBekreftelse(ny: Bekreftelse): BekreftelseTilstand {
    val nyBekreftelser = bekreftelser.map {
        if (it.bekreftelseId == ny.bekreftelseId) ny else it
    }
    return copy(bekreftelser = nyBekreftelser)
}

fun BekreftelseTilstand.leggTilNyEllerOppdaterBekreftelse(ny: Bekreftelse): BekreftelseTilstand {
    val nyBekreftelser = bekreftelser
        .filter { it.bekreftelseId != ny.bekreftelseId }
        .plus(ny)
    return copy(bekreftelser = nyBekreftelser)
}


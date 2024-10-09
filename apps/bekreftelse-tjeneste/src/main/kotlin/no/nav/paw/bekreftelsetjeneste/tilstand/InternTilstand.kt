package no.nav.paw.bekreftelsetjeneste.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode

@JvmRecord
data class InternTilstand(
    val periode: PeriodeInfo,
    val bekreftelser: List<Bekreftelse>
)

fun initTilstand(
    id: Long,
    key: Long,
    periode: Periode,
): InternTilstand =
    InternTilstand(
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

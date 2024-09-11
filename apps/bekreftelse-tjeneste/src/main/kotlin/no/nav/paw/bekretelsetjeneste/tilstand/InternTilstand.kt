package no.nav.paw.bekretelsetjeneste.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.Instant
import java.util.UUID

@JvmRecord
data class InternTilstand(
    val periode: PeriodeInfo,
    val bekreftelser: List<Bekreftelse>
)

@JvmRecord
data class Bekreftelse(
    val tilstand: Tilstand,
    val sistePurring: Instant?,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
)

sealed interface Tilstand{
    data object IkkeKlarForUtfylling: Tilstand
    data object KlarForUtfylling: Tilstand
    data object VenterSvar: Tilstand
    data object Levert : Tilstand
}


@JvmRecord
data class PeriodeInfo(
    val periodeId: UUID,
    val identitetsnummer: String,
    val arbeidsoekerId: Long,
    val recordKey: Long,
    val startet: Instant,
    val avsluttet: Instant?
) {
    val erAvsluttet: Boolean
        get() = avsluttet != null
}

fun initTilstand(
    id: Long,
    key: Long,
    periode: Periode
): InternTilstand =
    InternTilstand(
        periode = PeriodeInfo(
            periodeId = periode.id,
            identitetsnummer = periode.identitetsnummer,
            arbeidsoekerId = id,
            recordKey = key,
            startet = periode.startet.tidspunkt,
            avsluttet = periode.avsluttet.tidspunkt
        ),
        bekreftelser = emptyList()
    )

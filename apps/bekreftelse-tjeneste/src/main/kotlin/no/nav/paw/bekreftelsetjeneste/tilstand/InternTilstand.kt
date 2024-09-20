package no.nav.paw.bekreftelsetjeneste.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.Instant
import java.util.*

@JvmRecord
data class InternTilstand(
    val periode: PeriodeInfo,
    val bekreftelser: List<Bekreftelse>
)

@JvmRecord
data class Bekreftelse(
    val tilstand: Tilstand,
    val sisteVarselOmGjenstaaendeGraceTid: Instant?,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
)

enum class Tilstand {
    IkkeKlarForUtfylling,
    KlarForUtfylling,
    VenterSvar,
    GracePeriodeUtlopt,
    Levert
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

fun initBekreftelsePeriode(
    periode: PeriodeInfo
): Bekreftelse =
    Bekreftelse(
        tilstand = Tilstand.IkkeKlarForUtfylling,
        sisteVarselOmGjenstaaendeGraceTid = null,
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = periode.startet,
        gjelderTil = fristForNesteBekreftelse(periode.startet, BekreftelseConfig.bekreftelseInterval)
    )

fun List<Bekreftelse>.initNyBekreftelsePeriode(
): Bekreftelse =
    maxBy { it.gjelderTil }.copy(
        tilstand = Tilstand.KlarForUtfylling,
        sisteVarselOmGjenstaaendeGraceTid = null,
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = maxBy { it.gjelderTil }.gjelderTil,
        gjelderTil = fristForNesteBekreftelse(maxBy { it.gjelderTil }.gjelderTil, BekreftelseConfig.bekreftelseInterval)
    )
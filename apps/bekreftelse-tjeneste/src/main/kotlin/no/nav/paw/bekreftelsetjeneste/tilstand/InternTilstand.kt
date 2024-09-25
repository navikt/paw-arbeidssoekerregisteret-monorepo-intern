package no.nav.paw.bekreftelsetjeneste.tilstand

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
    val tilgjengeliggjort: Instant?,
    val fristUtloept: Instant?,
    val sisteVarselOmGjenstaaendeGraceTid: Instant?,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Tilstand.IkkeKlarForUtfylling::class, name = "IkkeKlarForUtfylling"),
    JsonSubTypes.Type(value = Tilstand.KlarForUtfylling::class, name = "KlarForUtfylling"),
    JsonSubTypes.Type(value = Tilstand.VenterSvar::class, name = "VenterSvar"),
    JsonSubTypes.Type(value = Tilstand.GracePeriodeUtloept::class, name = "GracePeriodeUtloept"),
    JsonSubTypes.Type(value = Tilstand.Levert::class, name = "Levert")
)
sealed class Tilstand {
    data object IkkeKlarForUtfylling : Tilstand()
    data object KlarForUtfylling : Tilstand()
    data object VenterSvar : Tilstand()
    data object GracePeriodeUtloept : Tilstand()
    data object Levert : Tilstand()
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
        tilgjengeliggjort = null,
        fristUtloept = null,
        sisteVarselOmGjenstaaendeGraceTid = null,
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = periode.startet,
        gjelderTil = fristForNesteBekreftelse(periode.startet, BekreftelseConfig.bekreftelseInterval)
    )

fun List<Bekreftelse>.initNewBekreftelse(
    tilgjengeliggjort: Instant,
): Bekreftelse =
    maxBy { it.gjelderTil }.copy(
        tilstand = Tilstand.KlarForUtfylling,
        tilgjengeliggjort = tilgjengeliggjort,
        sisteVarselOmGjenstaaendeGraceTid = null,
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = maxBy { it.gjelderTil }.gjelderTil,
        gjelderTil = fristForNesteBekreftelse(maxBy { it.gjelderTil }.gjelderTil, BekreftelseConfig.bekreftelseInterval)
    )
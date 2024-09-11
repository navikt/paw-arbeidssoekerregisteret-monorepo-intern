package no.nav.paw.meldeplikttjeneste.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass

@JvmRecord
data class InternTilstand(
    val periode: PeriodeInfo,
    val utestaaende: List<Rapportering>
)

@JvmRecord
data class Rapportering(
    val sisteHandling: KClass<BekreftelseHendelse>,
    val rapporteringsId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
)


@JvmRecord
data class PeriodeInfo(
    val periodeId: UUID,
    val identitetsnummer: String,
    val kafkaKeysId: Long,
    val recordKey: Long,
    val avsluttet: Boolean
)

fun initTilstand(
    id: Long,
    key: Long,
    periode: Periode
): InternTilstand =
    InternTilstand(
        periode = PeriodeInfo(
            periodeId = periode.id,
            identitetsnummer = periode.identitetsnummer,
            kafkaKeysId = id,
            recordKey = key,
            avsluttet = periode.avsluttet != null
        ),
        utestaaende = emptyList()
    )

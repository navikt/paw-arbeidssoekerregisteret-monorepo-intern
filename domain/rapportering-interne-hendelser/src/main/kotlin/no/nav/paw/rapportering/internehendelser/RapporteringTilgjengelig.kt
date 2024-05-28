package no.nav.paw.rapportering.internehendelser

import java.time.Instant
import java.util.*

const val rapporteringTilgjengeligHendelseType = "rapportering.tilgjengelig"

data class RapporteringTilgjengelig (
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val rapporteringsId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
) : RapporteringsHendelse {
    override val hendelseType: String = rapporteringTilgjengeligHendelseType
}
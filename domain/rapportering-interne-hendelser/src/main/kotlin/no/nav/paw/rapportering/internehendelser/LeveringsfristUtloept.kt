package no.nav.paw.rapportering.internehendelser

import java.util.*

const val leveringsfristUtloeptHendelseType = "rapportering.leveringsfrist_utloept"

data class LeveringsfristUtloept(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val rapporteringsId: UUID
) : RapporteringsHendelse {
    override val hendelseType: String = leveringsfristUtloeptHendelseType
}

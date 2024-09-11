package no.nav.paw.bekreftelse.internehendelser

import java.util.*

const val leveringsfristUtloeptHendelseType = "bekreftelse.leveringsfrist_utloept"

data class LeveringsfristUtloept(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    val bekreftelseId: UUID
) : BekreftelseHendelse {
    override val hendelseType: String = leveringsfristUtloeptHendelseType
}

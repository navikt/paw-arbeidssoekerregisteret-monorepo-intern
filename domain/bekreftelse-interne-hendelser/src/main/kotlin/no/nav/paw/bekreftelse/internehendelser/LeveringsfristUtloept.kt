package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

const val leveringsfristUtloeptHendelseType = "bekreftelse.leveringsfrist_utloept"

data class LeveringsfristUtloept(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
    val bekreftelseId: UUID,
    val leveringsfrist: Instant
) : BekreftelseHendelse {
    override val hendelseType: String = leveringsfristUtloeptHendelseType
}

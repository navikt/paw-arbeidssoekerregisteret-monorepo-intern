package no.nav.paw.bekreftelse.internehendelser

import java.time.Duration
import java.util.*

const val registerGracePeriodeGjenstaandeTid = "bekreftelse.register_grace_periode_gjenstaande_tid"

data class RegisterGracePeriodeGjendstaaendeTid(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    val bekreftelseId: UUID,
    val gjenstaandeTid: Duration
) : BekreftelseHendelse {
    override val hendelseType: String = registerGracePeriodeGjenstaandeTid
}

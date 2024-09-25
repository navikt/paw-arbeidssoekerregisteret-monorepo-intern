package no.nav.paw.bekreftelse.internehendelser

import java.time.Duration
import java.time.Instant
import java.util.*

const val registerGracePeriodeGjenstaaendeTid = "bekreftelse.register_grace_periode_gjenstaaende_tid"

data class RegisterGracePeriodeGjendstaaendeTid(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
    val bekreftelseId: UUID,
    val gjenstaandeTid: Duration
) : BekreftelseHendelse {
    override val hendelseType: String = registerGracePeriodeGjenstaaendeTid
}

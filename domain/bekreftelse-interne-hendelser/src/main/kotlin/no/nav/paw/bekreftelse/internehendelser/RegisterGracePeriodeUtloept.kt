package no.nav.paw.bekreftelse.internehendelser

import java.util.*

const val registerGracePeriodeUtloeptHendelseType = "bekreftelse.register_grace_periode_utloept"

data class RegisterGracePeriodeUtloept(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val bekreftelseId: UUID
) : BekreftelseHendelse {
    override val hendelseType: String = registerGracePeriodeUtloeptHendelseType
}

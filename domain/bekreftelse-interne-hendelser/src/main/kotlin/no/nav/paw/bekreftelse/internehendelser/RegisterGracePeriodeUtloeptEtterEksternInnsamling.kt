package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

const val registerGracePeriodeUtloeptEtterEksternInnsamlingHendelseType = "bekreftelse.register_grace_periode_utloept_etter_ekstern_innsamling"

data class RegisterGracePeriodeUtloeptEtterEksternInnsamling(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
    val kilde: String = "",
) : BekreftelseHendelse {
    override val hendelseType: String = registerGracePeriodeUtloeptEtterEksternInnsamlingHendelseType
}

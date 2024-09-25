package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

const val eksternGracePeriodeUtloeptHendelseType = "bekreftelse.ekstern_grace_periode_utloept"

data class EksternGracePeriodeUtloept(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
    val ansvarligNamespace: String,
    val ansvarligId: String
) : BekreftelseHendelse {
    override val hendelseType: String = eksternGracePeriodeUtloeptHendelseType
}

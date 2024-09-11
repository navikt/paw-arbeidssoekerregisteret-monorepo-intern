package no.nav.paw.bekreftelse.internehendelser

import java.util.*

const val eksternGracePeriodeUtloeptHendelseType = "bekreftelse.ekstern_grace_periode_utloept"

data class EksternGracePeriodeUtloept(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val ansvarligNamespace: String,
    val ansvarligId: String
) : BekreftelseHendelse {
    override val hendelseType: String = eksternGracePeriodeUtloeptHendelseType
}

package no.nav.paw.rapportering.internehendelser

import java.util.*

const val registerGracePeriodeUtloeptHendelseType = "rapportering.register_grace_periode_utloept"

data class RegisterGracePeriodeUtloept(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val rapporteringsId: UUID
) : RapporteringsHendelse {
    override val hendelseType: String = registerGracePeriodeUtloeptHendelseType
}

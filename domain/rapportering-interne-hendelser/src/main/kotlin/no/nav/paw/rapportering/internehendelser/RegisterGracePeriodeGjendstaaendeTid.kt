package no.nav.paw.rapportering.internehendelser

import java.time.Duration
import java.util.*

const val registerGracePeriodeGjenstaandeTid = "rapportering.register_grace_periode_gjenstaande_tid"

data class RegisterGracePeriodeGjendstaaendeTid(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val rapporteringsId: UUID,
    val gjenstaandeTid: Duration
) : RapporteringsHendelse {
    override val hendelseType: String = registerGracePeriodeGjenstaandeTid
}

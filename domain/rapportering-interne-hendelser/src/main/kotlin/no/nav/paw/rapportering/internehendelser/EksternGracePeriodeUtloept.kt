package no.nav.paw.rapportering.internehendelser

import java.util.*

const val eksternGracePeriodeUtloeptHendelseType = "rapportering.ekstern_grace_periode_utloept"

data class EksternGracePeriodeUtloept(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val ansvarligNamespace: String,
    val ansvarligId: String
) : RapporteringsHendelse {
    override val hendelseType: String = eksternGracePeriodeUtloeptHendelseType
}

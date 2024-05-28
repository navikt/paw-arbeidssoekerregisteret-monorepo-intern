package no.nav.paw.rapportering.internehendelser

import java.util.*


const val periodeAvsluttetHendelsesType = "rapportering.periode_avsluttet"

data class PeriodeAvsluttet(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long
    ) : RapporteringsHendelse {
    override val hendelseType: String = periodeAvsluttetHendelsesType
}

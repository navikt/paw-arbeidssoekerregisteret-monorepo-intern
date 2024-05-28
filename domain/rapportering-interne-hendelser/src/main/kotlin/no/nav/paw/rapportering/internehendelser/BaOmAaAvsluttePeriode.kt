package no.nav.paw.rapportering.internehendelser

import java.util.*

const val beOmAaAvsluttePeriodeHendelsesType = "rapportering.be_om_aa_avslutte_periode"

class BaOmAaAvsluttePeriode(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
) : RapporteringsHendelse {
    override val hendelseType: String = beOmAaAvsluttePeriodeHendelsesType
}
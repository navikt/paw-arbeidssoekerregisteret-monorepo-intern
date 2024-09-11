package no.nav.paw.bekreftelse.internehendelser

import java.util.*

const val beOmAaAvsluttePeriodeHendelsesType = "bekreftelse.be_om_aa_avslutte_periode"

class BaOmAaAvsluttePeriode(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
) : BekreftelseHendelse {
    override val hendelseType: String = beOmAaAvsluttePeriodeHendelsesType
}
package no.nav.paw.bekreftelse.internehendelser

import no.nav.paw.bekreftelse.internehendelser.vo.Bruker
import java.time.Instant
import java.util.*

const val baOmAaAvsluttePeriodeHendelsesType = "bekreftelse.ba_om_aa_avslutte_periode"

class BaOmAaAvsluttePeriode(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
    val utfoertAv: Bruker
) : BekreftelseHendelse {
    override val hendelseType: String = baOmAaAvsluttePeriodeHendelsesType
}
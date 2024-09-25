package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*


const val periodeAvsluttetHendelsesType = "bekreftelse.periode_avsluttet"

data class PeriodeAvsluttet(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant
) : BekreftelseHendelse {
    override val hendelseType: String = periodeAvsluttetHendelsesType
}

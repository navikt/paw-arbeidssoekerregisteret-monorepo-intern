package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

const val meldingMottattHendelseType = "bekreftelse.melding_mottatt"

data class BekreftelseMeldingMottatt(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
    val bekreftelseId: UUID
) : BekreftelseHendelse {
    override val hendelseType: String = meldingMottattHendelseType
}
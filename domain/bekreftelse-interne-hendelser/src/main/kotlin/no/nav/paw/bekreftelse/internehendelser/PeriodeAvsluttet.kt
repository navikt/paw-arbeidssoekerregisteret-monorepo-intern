package no.nav.paw.bekreftelse.internehendelser

import java.util.*


const val periodeAvsluttetHendelsesType = "bekreftelse.periode_avsluttet"

data class PeriodeAvsluttet(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long
    ) : BekreftelseHendelse {
    override val hendelseType: String = periodeAvsluttetHendelsesType
}

package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

const val andreHarOvertattAnsvarHendelsesType = "bekreftelse.andre_har_overtatt_ansvar"

class AndreHarOvertattAnsvar(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
) : BekreftelseHendelse {
    override val hendelseType: String = andreHarOvertattAnsvarHendelsesType
}
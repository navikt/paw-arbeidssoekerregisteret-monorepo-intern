package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

const val bekreftelseTilgjengeligHendelseType = "bekreftelse.tilgjengelig"

data class BekreftelseTilgjengelig (
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
) : BekreftelseHendelse {
    override val hendelseType: String = bekreftelseTilgjengeligHendelseType
}
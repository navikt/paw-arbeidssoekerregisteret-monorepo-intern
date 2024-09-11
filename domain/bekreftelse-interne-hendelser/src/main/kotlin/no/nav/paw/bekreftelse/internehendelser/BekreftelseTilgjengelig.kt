package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

const val bekreftelseTilgjengeligHendelseType = "bekreftelse.tilgjengelig"

data class BekreftelseTilgjengelig (
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val identitetsnummer: String,
    override val arbeidssoekerId: Long,
    val bekreftelseId: UUID,
    val gjelderFra: Instant,
    val gjelderTil: Instant
) : BekreftelseHendelse {
    override val hendelseType: String = bekreftelseTilgjengeligHendelseType
}
package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

const val bekreftelsePaaVegneAvStartetHendelsesType = "bekreftelse.paa_vegne_av_startet"

class BekreftelsePaaVegneAvStartet(
    override val hendelseId: UUID,
    override val periodeId: UUID,
    override val arbeidssoekerId: Long,
    override val hendelseTidspunkt: Instant,
) : BekreftelseHendelse {
    override val hendelseType: String = bekreftelsePaaVegneAvStartetHendelsesType
}
package no.nav.paw.arbeidssokerregisteret.intern.v1

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Situasjon
import java.util.*

data class SituasjonMottatt(
    override val hendelseId: UUID,
    override val metadata: Metadata,
    override val identitetsnummer: String,
    val situasjon: Situasjon
): Hendelse {
    override val hendelseType: HendelseType = situasjonMottattHendelseType
}
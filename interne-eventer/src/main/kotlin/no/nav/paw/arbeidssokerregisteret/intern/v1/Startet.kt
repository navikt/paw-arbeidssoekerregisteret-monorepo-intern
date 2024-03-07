package no.nav.paw.arbeidssokerregisteret.intern.v1

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import java.util.*

class Startet(
    override val hendelseId: UUID,
    override val id: Long,
    override val identitetsnummer: String,
    override val metadata: Metadata
): Hendelse {
    override val hendelseType: String = startetHendelseType
}
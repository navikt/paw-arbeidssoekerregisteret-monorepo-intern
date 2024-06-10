package no.nav.paw.arbeidssokerregisteret.intern.v1

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import java.util.*

data class Startet(
    override val hendelseId: UUID,
    override val id: Long,
    override val identitetsnummer: String,
    override val metadata: Metadata,
    override val opplysninger: Set<Opplysning> = emptySet()
): Hendelse, HarOpplysninger {
    override val hendelseType: String = startetHendelseType
}
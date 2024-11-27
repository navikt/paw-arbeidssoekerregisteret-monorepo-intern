package no.nav.paw.arbeidssokerregisteret.intern.v1

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import java.util.*

data class IdentitetsnummerSammenslaatt(
    override val id: Long,
    override val hendelseId: UUID,
    override val identitetsnummer: String,
    override val metadata: Metadata,
    val flyttedeIdentitetsnumre: Set<String>,
    val flyttetTilArbeidssoekerId: Long
): Hendelse {
    init {
        require(identitetsnummer in flyttedeIdentitetsnumre) { "IdentitetsnummerSammenslaatt: identitetsnummer må være en av alleIdentitetsnummer" }
    }
    override val hendelseType: String = identitetsnummerSammenslaattHendelseType
}
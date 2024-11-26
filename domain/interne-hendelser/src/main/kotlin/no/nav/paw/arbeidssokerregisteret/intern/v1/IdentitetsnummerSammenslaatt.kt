package no.nav.paw.arbeidssokerregisteret.intern.v1

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import java.util.*

data class IdentitetsnummerSammenslaatt(
    override val id: Long,
    override val hendelseId: UUID,
    override val identitetsnummer: String,
    override val metadata: Metadata,
    val alleIdentitetsnummer: List<String>,
    val flyttetTilArbeidssoekerId: Long
): Hendelse {
    init {
        require(identitetsnummer in alleIdentitetsnummer) { "IdentitetsnummerSammenslaatt: identitetsnummer må være en av alleIdentitetsnummer" }
    }
    override val hendelseType: String = identitetsnummerSammenslaattHendelseType
}
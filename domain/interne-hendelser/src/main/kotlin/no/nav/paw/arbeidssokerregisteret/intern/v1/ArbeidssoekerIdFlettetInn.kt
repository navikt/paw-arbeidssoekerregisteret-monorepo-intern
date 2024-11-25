package no.nav.paw.arbeidssokerregisteret.intern.v1

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import java.util.*

data class ArbeidssoekerIdFlettetInn(
    override val identitetsnummer: String,
    override val id: Long,
    override val hendelseId: UUID,
    override val metadata: Metadata,
    val kilde: Kilde
): Hendelse {
    override val hendelseType: String = arbeidssoekerIdFlettetInn
}

data class Kilde(
    val arbeidssoekerId: Long,
    val identitetsnummer: Set<String>
)

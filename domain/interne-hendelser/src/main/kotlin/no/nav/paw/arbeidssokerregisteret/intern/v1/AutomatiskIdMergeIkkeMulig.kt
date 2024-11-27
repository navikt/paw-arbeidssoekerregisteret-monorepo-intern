package no.nav.paw.arbeidssokerregisteret.intern.v1

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import java.time.Instant
import java.util.*

class AutomatiskIdMergeIkkeMulig(
    override val identitetsnummer: String,
    override val id: Long,
    override val hendelseId: UUID,
    override val metadata: Metadata,
    val gjeldeneIdentitetsnummer: String?,
    val pdlIdentitetsnummer: List<String>,
    val lokaleAlias: List<Alias>,
    val perioder: List<PeriodeRad>
) : Hendelse {
    override val hendelseType: String = automatiskIdMergeIkkeMulig
}

data class Alias(
    val identitetsnummer: String,
    val arbeidsoekerId: Long,
    val recordKey: Long,
    val partition: Int,
)

data class PeriodeRad(
    val periodeId: UUID,
    val identitetsnummer: String,
    val fra: Instant,
    val til: Instant?
) {
    val erAktiv: Boolean = til == null
}

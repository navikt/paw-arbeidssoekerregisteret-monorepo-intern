package no.nav.paw.identitet.internehendelser

import no.nav.paw.identitet.internehendelser.vo.Identitet
import java.time.Instant
import java.util.*

data class IdentiteterSlettetHendelse(
    val tidligereIdentiteter: List<Identitet> = emptyList(),
    override val hendelseId: UUID = UUID.randomUUID(),
    override val hendelseTidspunkt: Instant = Instant.now()
) : IdentitetHendelse {
    override val hendelseType: String = IDENTITETER_SLETTET_V1_HENDELSE_TYPE
}

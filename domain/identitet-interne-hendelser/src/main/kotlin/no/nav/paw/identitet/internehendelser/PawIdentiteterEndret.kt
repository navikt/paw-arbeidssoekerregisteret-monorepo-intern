package no.nav.paw.identitet.internehendelser

import no.nav.paw.identitet.internehendelser.vo.Identitet
import java.time.Instant
import java.util.*

data class PawIdentiteterEndret(
    val identiteter: List<Identitet>,
    val historiskeIdentiteter: List<Identitet> = emptyList(),
    override val hendelseId: UUID = UUID.randomUUID(),
    override val hendelseTidspunkt: Instant = Instant.now()
) : IdentitetHendelse {
    override val hendelseType: String = PAW_IDENTITETER_ENDRET_HENDELSE_TYPE
}

package no.nav.paw.identitet.internehendelser

import java.time.Instant
import java.util.*

const val IDENTITETER_ENDRET_HENDELSE_TYPE = "identitet.identiteter_endret"

sealed interface IdentitetHendelse {
    val hendelseId: UUID
    val hendelseType: String
    val hendelseTidspunkt: Instant
}


package no.nav.paw.identitet.internehendelser

import java.time.Instant
import java.util.*

const val IDENTITETER_ENDRET_HENDELSE_TYPE = "identitet.identiteter_endret"
const val IDENTITETER_MERGET_HENDELSE_TYPE = "identitet.identiteter_merget"

sealed interface IdentitetHendelse {
    val hendelseId: UUID
    val hendelseType: String
    val hendelseTidspunkt: Instant
}


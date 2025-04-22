package no.nav.paw.identitet.internehendelser

import java.time.Instant
import java.util.*

const val PAW_IDENTITETER_ENDRET_HENDELSE_TYPE = "identitet.paw_identiteter_endret"
const val PDL_IDENTITETER_ENDRET_HENDELSE_TYPE = "identitet.pdl_identiteter_endret"

sealed interface IdentitetHendelse {
    val hendelseId: UUID
    val hendelseType: String
    val hendelseTidspunkt: Instant
}


package no.nav.paw.identitet.internehendelser

import java.time.Instant
import java.util.*

const val IDENTITETER_ENDRET_V1_HENDELSE_TYPE = "identitet.v1.identiteter_endret"
const val IDENTITETER_MERGET_V1_HENDELSE_TYPE = "identitet.v1.identiteter_merget"
const val IDENTITETER_SPLITTET_V1_HENDELSE_TYPE = "identitet.v1.identiteter_splittet"
const val IDENTITETER_SLETTET_V1_HENDELSE_TYPE = "identitet.v1.identiteter_slettet"

sealed interface IdentitetHendelse {
    val hendelseId: UUID
    val hendelseType: String
    val hendelseTidspunkt: Instant
}


package no.nav.paw.kafkakeygenerator.model

import no.nav.person.pdl.aktor.v2.Type

data class Identitet(
    val arbeidssoekerId: Long? = null,
    val aktorId: String,
    val identitet: String,
    val type: IdentitetType,
    val gjeldende: Boolean,
    val status: IdentitetStatus
)

enum class IdentitetType {
    FOLKEREGISTERIDENT, AKTORID, NPID
}

enum class IdentitetStatus {
    PENDING, PROCESSING, VERIFIED, CONFLICT, DELETED
}

fun IdentitetRow.asIdentitet(): Identitet = Identitet(
    arbeidssoekerId = arbeidssoekerId,
    aktorId = aktorId,
    identitet = identitet,
    type = type,
    gjeldende = gjeldende,
    status = status,
)

fun Type.asIdentitetType(): IdentitetType = when (this) {
    Type.NPID -> IdentitetType.NPID
    Type.AKTORID -> IdentitetType.AKTORID
    Type.FOLKEREGISTERIDENT -> IdentitetType.FOLKEREGISTERIDENT
}

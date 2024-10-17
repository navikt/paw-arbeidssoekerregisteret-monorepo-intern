package no.nav.paw.kafkakeygenerator.api.v2

data class InfoResponse(
    val lagretData: LokalIdData?,
    val pdlData: PdlData
)

data class LokalIdData(
    val arbeidsoekerId: Long,
    val recordKey: Long
)

data class PdlData(
    val error: String?,
    val id: List<PdlId>?
)

data class PdlId(
    val gruppe: String,
    val id: String
)
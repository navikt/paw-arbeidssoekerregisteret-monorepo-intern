package no.nav.paw.kafkakeygenerator.model

data class HentAliasResponse(
    val alias: List<LokaleAliasResponse>
)

data class LokaleAliasResponse(
    val identitetsnummer: String,
    val koblinger: List<AliasResponse>
)

data class AliasResponse(
    val identitetsnummer: String,
    val arbeidsoekerId: Long,
    val recordKey: Long,
    val partition: Int,
)

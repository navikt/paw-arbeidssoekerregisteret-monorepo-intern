package no.nav.paw.kafkakeygenerator.client


data class AliasRequest(
    val antallPartisjoner: Int,
    val identer: List<String>
)

data class AliasResponse(
    val alias: List<LokaleAlias>
)

data class LokaleAlias(
    val identitetsnummer: String,
    val kobliner: List<Alias>
)

data class Alias(
    val identitetsnummer: String,
    val arbeidsoekerId: Long,
    val recordKey: Long,
    val partition: Int,
)

package no.nav.paw.kafkakeygenerator.api.v2

typealias AliasResponse = List<LokaleAlias>

data class LokaleAlias(
    val identitetsnummer: String,
    val koblinger: List<Alias>
)

data class Alias(
    val identitetsnummer: String,
    val arbeidsoekerId: Long,
    val recordKey: Long,
    val partition: Int,
)
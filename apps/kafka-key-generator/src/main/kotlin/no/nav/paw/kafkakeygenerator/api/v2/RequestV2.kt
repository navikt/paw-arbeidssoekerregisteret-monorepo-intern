package no.nav.paw.kafkakeygenerator.api.v2

data class RequestV2(
    val ident: String
)

data class AliasRequest(
    val antallPartisjoner: Int,
    val identer: List<String>
)
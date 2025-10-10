package no.nav.paw.kafkakeygenerator.api.v2

data class AliasRequest(
    val antallPartisjoner: Int,
    val identer: List<String>
)

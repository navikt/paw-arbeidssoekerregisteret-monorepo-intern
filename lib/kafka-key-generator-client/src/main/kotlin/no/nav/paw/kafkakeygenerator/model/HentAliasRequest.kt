package no.nav.paw.kafkakeygenerator.model

data class HentAliasRequest(
    val antallPartisjoner: Int,
    val identer: List<String>
)

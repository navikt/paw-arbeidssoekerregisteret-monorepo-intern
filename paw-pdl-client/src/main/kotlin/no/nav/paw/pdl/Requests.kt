package no.nav.paw.pdl

import kotlinx.serialization.Serializable

@Serializable
internal data class PdlQuery(
    val query: String,
    val variables: Variables
)

@Serializable
internal data class Variables(
    val ident: String
)

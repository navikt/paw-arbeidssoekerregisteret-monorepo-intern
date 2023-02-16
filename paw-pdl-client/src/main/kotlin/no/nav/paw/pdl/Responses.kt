@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.paw.pdl

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
internal data class PdlResponse<T>(
    val errors: List<PdlError>?,
    val data: T?
)

@Serializable
data class PdlHentIdenter(
    val hentIdenter: PdlIdentResponse
)

/**
 * Tilsvarer graphql-spørringen hentIdenter.graphql
 */

@Serializable
data class PdlHentFullPerson(
    val hentIdenter: PdlIdentResponse?
)

@Serializable
data class PdlIdentResponse(val identer: List<PdlIdent>) {
    fun trekkUtIdent(gruppe: PdlIdent.PdlIdentGruppe): String? = identer.firstOrNull { it.gruppe == gruppe }?.ident
}

@Serializable
data class PdlIdent(val ident: String, val gruppe: PdlIdentGruppe) {
    enum class PdlIdentGruppe { AKTORID, FOLKEREGISTERIDENT, NPID }
}

@Serializable
data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension
)

@Serializable
data class PdlErrorLocation(
    val line: Int?,
    val column: Int?
)

@Serializable
data class PdlErrorExtension(
    val code: String?,
    val classification: String
)

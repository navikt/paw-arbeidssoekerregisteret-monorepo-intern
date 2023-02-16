package no.nav.paw.pdl

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Enkel GraphQL-klient for PDL som kan enten hente navn fra aktør eller fnr (ident)
 * eller hente mer fullstendig data om en person via fnr eller aktørid (ident)
 *
 * Authorisasjon gjøres via den gitte Token prodvideren, og servicebrukeren som er angitt i token provideren må være i
 * i AD-gruppen `0000-GA-TEMA_SYK` som dokumentert [her](https://pdldocs-navno.msappproxy.net/intern/index.html#_konsumentroller_basert_p%C3%A5_tema).
 *
 * Klienten vil alltid gi PDL-Temaet 'SYK', så om du trenger et annet tema må du endre denne klienten.
 */
class PdlClient(
    private val url: String,
    private val getAccessToken: () -> String
) {
    private val httpClient = createHttpClient()

    private val hentIdenterQuery = "hentIdenter.graphql".readQuery()

    suspend fun hentIdenter(ident: String, userLoginToken: String? = null): String? =
        PdlQuery(hentIdenterQuery, Variables(ident))
            .execute<PdlHentIdenter>(userLoginToken)
            ?.hentIdenter
            ?.trekkUtIdent(PdlIdent.PdlIdentGruppe.AKTORID)

    // Funksjonen må være inline+reified for å kunne deserialisere T
    private suspend inline fun <reified T> PdlQuery.execute(userLoginToken: String?): T? {
        val stsToken = getAccessToken()

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            bearerAuth(userLoginToken ?: stsToken)
            header("Tema", "OPP")

            setBody(this@execute)
        }
            .body<PdlResponse<T>>()

        if (!response.errors.isNullOrEmpty()) {
            throw PdlException(response.errors)
        }

        return response.data
    }
}

class PdlException(val errors: List<PdlError>?) : RuntimeException()

private fun String.readQuery(): String =
    this.readResource().replace(Regex("[\r\n]"), "")

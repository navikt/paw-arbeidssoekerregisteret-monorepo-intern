package no.nav.paw.bekreftelse.api.utils

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.paw.bekreftelse.api.exception.DataIkkeFunnetForIdException
import no.nav.paw.bekreftelse.api.models.TilgjengeligBekreftelse
import no.nav.paw.error.plugin.installErrorHandlingPlugin
import no.nav.paw.serialization.plugin.installContentNegotiationPlugin
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at HTTP-svarene fra bekreftelse-api har samme JSON-format som da appen brukte Jackson 2.
 * Bruker samme JSON- og feilhåndteringsoppsett som produksjon. JSON-filene er generert med
 * Jackson 2, se [lagretJson].
 */
class HttpSvarKompatibilitetTest : FreeSpec({
    svar.forEach { (navn, sti) ->
        "$navn har samme JSON-format som med Jackson 2" {
            testApplication {
                application {
                    installContentNegotiationPlugin()
                    installErrorHandlingPlugin()
                    routing {
                        get("/liste") { call.respond(listOf(tilgjengeligBekreftelse)) }
                        get("/tom") { call.respond(emptyList<TilgjengeligBekreftelse>()) }
                        get("/feil") { throw DataIkkeFunnetForIdException("Fant ingen bekreftelse for gitt id") }
                    }
                }
                val faktisk = client.get(sti).bodyAsText()
                val lagret = lagretJson("bekreftelse-api/http-$navn") { faktisk }

                faktisk.utenVarierendeVerdier() shouldBe lagret.utenVarierendeVerdier()
            }
        }
    }
})

private val svar = listOf(
    "tilgjengelige-bekreftelser" to "/liste",
    "tilgjengelige-bekreftelser-tom" to "/tom",
    "feilsvar" to "/feil"
)

private val tilgjengeligBekreftelse = TilgjengeligBekreftelse(
    periodeId = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"),
    bekreftelseId = UUID.fromString("5f6a7b8c-9d0e-4f1a-8b2c-3d4e5f6a7b8c"),
    gjelderFra = Instant.parse("2025-03-02T23:00:00Z"),
    gjelderTil = Instant.parse("2025-03-16T23:00:00.123456789Z")
)

/** `id` og `timestamp` i feilsvar genereres på nytt for hvert kall. Formatet sjekkes, ikke verdien. */
private fun String.utenVarierendeVerdier(): String = this
    .replace(Regex("\"id\":\"[0-9a-f-]{36}\""), "\"id\":\"<uuid>\"")
    .replace(Regex("\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z\""), "\"timestamp\":\"<iso-tidspunkt>\"")

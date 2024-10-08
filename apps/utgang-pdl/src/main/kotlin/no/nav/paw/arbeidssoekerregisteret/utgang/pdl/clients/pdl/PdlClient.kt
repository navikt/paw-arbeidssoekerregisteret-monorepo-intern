package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.PdlException
import no.nav.paw.pdl.graphql.generated.hentforenkletstatusbolk.HentPersonBolkResult as ForenkletStatusBolkResult
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult
import no.nav.paw.pdl.hentForenkletStatusBolk
import no.nav.paw.pdl.hentPersonBolk
import org.slf4j.LoggerFactory

const val BEHANDLINGSNUMMER = "B452"

fun interface PdlHentPerson {
    fun hentPerson(ident: List<String>, callId: String, navConsumerId: String): List<HentPersonBolkResult>?

    companion object {
        val logger = LoggerFactory.getLogger("pdlClient")

        fun create(): PdlHentPerson {
            val pdlClient = createPdlClient()
            return PdlHentPerson { ident, callId, navConsumerId ->
                runBlocking {
                    try {
                        pdlClient.hentPersonBolk(ident = ident, callId = callId, navConsumerId = navConsumerId, behandlingsnummer = BEHANDLINGSNUMMER)
                    } catch (e: PdlException) {
                        logger.error("PDL hentPerson feiler med: $e", e)
                        null
                    }
                }
            }
        }
    }
}

fun interface PdlHentForenkletStatus {
    fun hentForenkletStatus(ident: List<String>, callId: String, navConsumerId: String): List<ForenkletStatusBolkResult>?

    companion object {
        val logger = LoggerFactory.getLogger("pdlClient")

        fun create(): PdlHentForenkletStatus {
            val pdlClient = createPdlClient()
            return PdlHentForenkletStatus { ident, callId, navConsumerId ->
                runBlocking {
                    try {
                        pdlClient.hentForenkletStatusBolk(ident = ident, callId = callId, navConsumerId = navConsumerId, behandlingsnummer = BEHANDLINGSNUMMER)
                    } catch (e: PdlException) {
                        logger.error("PDL hentForenkletStatus feiler med: $e", e)
                        null
                    }

                }
            }
        }
    }
}

private fun createPdlClient(): PdlClient {
    val azureM2MConfig = loadNaisOrLocalConfiguration<AzureM2MConfig>("azure_m2m.toml")
    val m2mTokenClient = azureAdM2MTokenClient(currentRuntimeEnvironment, azureM2MConfig)
    val pdlConfig = loadNaisOrLocalConfiguration<PdlConfig>(PDL_CONFIG_FILE)

    return PdlClient(pdlConfig.url, pdlConfig.tema, createHttpClient()) {
        m2mTokenClient.createMachineToMachineToken(pdlConfig.scope)
    }
}

private fun createHttpClient() = HttpClient {
    install(ContentNegotiation) {
        jackson()
    }
}
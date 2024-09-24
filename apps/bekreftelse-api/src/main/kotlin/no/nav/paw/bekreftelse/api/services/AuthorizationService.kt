package no.nav.paw.bekreftelse.api.services

import no.nav.paw.bekreftelse.api.authz.AccessToken
import no.nav.paw.bekreftelse.api.authz.Azure
import no.nav.paw.bekreftelse.api.authz.NavIdent
import no.nav.paw.bekreftelse.api.authz.OID
import no.nav.paw.bekreftelse.api.authz.PID
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.exception.BrukerHarIkkeTilgangException
import no.nav.paw.bekreftelse.api.model.BrukerType
import no.nav.paw.bekreftelse.api.model.InnloggetBruker
import no.nav.paw.bekreftelse.api.model.NavAnsatt
import no.nav.paw.bekreftelse.api.model.Sluttbruker
import no.nav.paw.bekreftelse.api.utils.audit
import no.nav.paw.bekreftelse.api.utils.auditLogger
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.poao_tilgang.client.NavAnsattTilgangTilEksternBrukerPolicyInput
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.TilgangType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AuthorizationService(
    private val applicationConfig: ApplicationConfig,
    private val kafkaKeysClient: KafkaKeysClient,
    private val poaoTilgangClient: PoaoTilgangClient
) {
    private val logger: Logger = LoggerFactory.getLogger("no.nav.paw.logger.auth")

    suspend fun resolveSluttbruker(accessToken: AccessToken, identitetsnummer: String?): Sluttbruker {
        val sluttbrukerIdentitetsnummer = when (accessToken.issuer) {
            is Azure -> {
                // Veiledere skal alltid sende inn identitetsnummer for sluttbruker
                identitetsnummer
                    ?: throw BrukerHarIkkeTilgangException("Veileder må sende med identitetsnummer for sluttbruker")
            }

            else -> {
                val pid = accessToken[PID].verdi
                if (identitetsnummer != null && identitetsnummer != pid) {
                    // TODO Håndtere verge
                    throw BrukerHarIkkeTilgangException("Bruker har ikke tilgang til sluttbrukers informasjon")
                }
                identitetsnummer ?: pid
            }
        }

        val kafkaKeysResponse = kafkaKeysClient.getIdAndKey(sluttbrukerIdentitetsnummer)

        return Sluttbruker(
            identitetsnummer = sluttbrukerIdentitetsnummer,
            arbeidssoekerId = kafkaKeysResponse.id,
            kafkaKey = kafkaKeysResponse.key
        )
    }

    fun resolveInnloggetBruker(bearerToken: String, accessToken: AccessToken): InnloggetBruker {
        return when (accessToken.issuer) {
            is Azure -> {
                val ident = accessToken[NavIdent]
                InnloggetBruker(
                    type = BrukerType.VEILEDER,
                    ident = ident,
                    bearerToken = bearerToken
                )
            }

            else -> {
                val ident = accessToken[PID].verdi
                InnloggetBruker(
                    type = BrukerType.SLUTTBRUKER,
                    ident = ident,
                    bearerToken = bearerToken
                )
            }
        }
    }

    fun authorize(
        accessToken: AccessToken,
        sluttbruker: Sluttbruker,
        tilgangType: TilgangType
    ) {
        when (accessToken.issuer) {
            is Azure -> {
                val navAnsatt = accessToken.toNavAnsatt()

                val navAnsattTilgang = poaoTilgangClient.evaluatePolicy(
                    NavAnsattTilgangTilEksternBrukerPolicyInput(
                        navAnsattAzureId = navAnsatt.azureId,
                        tilgangType = tilgangType,
                        norskIdent = sluttbruker.identitetsnummer
                    )
                )
                val tilgang = navAnsattTilgang.getOrDefault {
                    throw BrukerHarIkkeTilgangException("Kunne ikke finne tilgang for ansatt")
                }

                if (tilgang.isDeny) {
                    throw BrukerHarIkkeTilgangException("NAV-ansatt har ikke $tilgangType-tilgang til bruker")
                } else {
                    logger.debug("NAV-ansatt har benyttet {}-tilgang til informasjon om bruker", tilgangType)
                    auditLogger.audit(
                        applicationConfig.runtimeEnvironment,
                        sluttbruker.identitetsnummer,
                        navAnsatt,
                        tilgangType,
                        "NAV-ansatt har benyttet $tilgangType-tilgang til informasjon om bruker"
                    )
                }
            }

            else -> {
                // TODO Håndtere verge
                logger.debug("Ingen tilgangssjekk for sluttbruker")
            }
        }
    }

    private fun AccessToken.toNavAnsatt() = NavAnsatt(azureId = this[OID], navIdent = this[NavIdent])
}


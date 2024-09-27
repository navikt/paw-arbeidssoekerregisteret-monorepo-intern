package no.nav.paw.bekreftelse.api.services

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.api.config.ApplicationConfig
import no.nav.paw.bekreftelse.api.context.RequestContext
import no.nav.paw.bekreftelse.api.context.SecurityContext
import no.nav.paw.bekreftelse.api.exception.BearerTokenManglerException
import no.nav.paw.bekreftelse.api.exception.BrukerHarIkkeTilgangException
import no.nav.paw.bekreftelse.api.exception.UgyldigBearerTokenException
import no.nav.paw.bekreftelse.api.model.AccessToken
import no.nav.paw.bekreftelse.api.model.Azure
import no.nav.paw.bekreftelse.api.model.BrukerType
import no.nav.paw.bekreftelse.api.model.Claims
import no.nav.paw.bekreftelse.api.model.InnloggetBruker
import no.nav.paw.bekreftelse.api.model.NavAnsatt
import no.nav.paw.bekreftelse.api.model.NavIdent
import no.nav.paw.bekreftelse.api.model.OID
import no.nav.paw.bekreftelse.api.model.PID
import no.nav.paw.bekreftelse.api.model.Sluttbruker
import no.nav.paw.bekreftelse.api.model.resolveTokens
import no.nav.paw.bekreftelse.api.utils.audit
import no.nav.paw.bekreftelse.api.utils.buildAuditLogger
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
    private val auditLogger: Logger = buildAuditLogger

    @WithSpan
    suspend fun authorize(requestContext: RequestContext, tilgangType: TilgangType): SecurityContext {
        val tokenContext = requestContext.principal?.context ?: throw BearerTokenManglerException("Sesjon mangler")

        val accessToken = tokenContext.resolveTokens().firstOrNull()
            ?: throw UgyldigBearerTokenException("Ingen gyldige Bearer Tokens funnet")

        if (accessToken.claims.isEmpty()) {
            throw UgyldigBearerTokenException("Bearer Token mangler påkrevd innhold")
        }

        val securityContext = SecurityContext(
            sluttbruker = resolveSluttbruker(accessToken, requestContext.identitetsnummer),
            innloggetBruker = resolveInnloggetBruker(accessToken),
            accessToken = accessToken,
            tilgangType = tilgangType
        )

        return authorize(securityContext)
    }

    private fun authorize(securityContext: SecurityContext): SecurityContext {
        val (sluttbruker, _, accessToken, tilgangType) = securityContext

        when (accessToken.issuer) {
            is Azure -> {
                val navAnsatt = accessToken.claims.toNavAnsatt()

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

        return securityContext
    }

    private suspend fun resolveSluttbruker(accessToken: AccessToken, identitetsnummer: String?): Sluttbruker {
        val sluttbrukerIdentitetsnummer = when (accessToken.issuer) {
            is Azure -> {
                // Veiledere skal alltid sende inn identitetsnummer for sluttbruker
                identitetsnummer
                    ?: throw BrukerHarIkkeTilgangException("Veileder må sende med identitetsnummer for sluttbruker")
            }

            else -> {
                val pid = accessToken.claims[PID].verdi
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

    private fun resolveInnloggetBruker(accessToken: AccessToken): InnloggetBruker {
        return when (accessToken.issuer) {
            is Azure -> {
                val ident = accessToken.claims[NavIdent]
                InnloggetBruker(
                    type = BrukerType.VEILEDER,
                    ident = ident
                )
            }

            else -> {
                val ident = accessToken.claims[PID].verdi
                InnloggetBruker(
                    type = BrukerType.SLUTTBRUKER,
                    ident = ident
                )
            }
        }
    }

    private fun Claims.toNavAnsatt() = NavAnsatt(azureId = this[OID], navIdent = this[NavIdent])
}


package no.nav.paw.arbeidssokerregisteret

import io.ktor.client.HttpClient
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.config.NaisEnv
import no.nav.paw.arbeidssokerregisteret.services.ArbeidssokerService
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.utils.createMockRSAKey
import no.nav.paw.arbeidssokerregisteret.utils.konfigVerdi
import no.nav.paw.pdl.PdlClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient

fun createDependencies(env: Map<String, String>, config: Config): Dependencies {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val azureAdMachineToMachineTokenClient =
        when (config.naisEnv) {
            NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
                .withClientId(env.konfigVerdi("AZURE_APP_CLIENT_ID"))
                .withPrivateJwk(createMockRSAKey("azure"))
                .withTokenEndpointUrl(env.konfigVerdi("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"))
                .buildMachineToMachineTokenClient()

            else -> AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildMachineToMachineTokenClient()
        }

    val pdlClient = PdlClient(
        config.pdlClient.url,
        "OPP",
        HttpClient()
    ) { azureAdMachineToMachineTokenClient.createMachineToMachineToken(config.pdlClient.scope) }

    val poaoTilgangCachedClient = PoaoTilgangHttpClient(
        config.poaoTilgangClient.url,
        { azureAdMachineToMachineTokenClient.createMachineToMachineToken(config.poaoTilgangClient.scope) }
    )

    val autorisasjonService = AutorisasjonService(poaoTilgangCachedClient)
    val arbeidssokerService = ArbeidssokerService(pdlClient)

    return Dependencies(
        registry,
        autorisasjonService,
        arbeidssokerService
    )
}

data class Dependencies(
    val registry: PrometheusMeterRegistry,
    val autorisasjonService: AutorisasjonService,
    val arbeidssokerService: ArbeidssokerService
)

package no.nav.paw.arbeidssokerregisteret

import io.ktor.client.HttpClient
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.config.NaisEnv
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.kafka.producers.NonBlockingKafkaProducer
import no.nav.paw.arbeidssokerregisteret.services.ArbeidssokerService
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.utils.createMockRSAKey
import no.nav.paw.pdl.PdlClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.clients.producer.KafkaProducer

fun createDependencies(config: Config): Dependencies {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val azureAdMachineToMachineTokenClient =
        when (config.naisEnv) {
            NaisEnv.Local -> AzureAdTokenClientBuilder.builder()
                .withClientId(config.authProviders.azure.clientId)
                .withPrivateJwk(createMockRSAKey("azure"))
                .withTokenEndpointUrl(config.authProviders.azure.tokenEndpointUrl)
                .buildMachineToMachineTokenClient()

            else -> AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildMachineToMachineTokenClient()
        }

    val pdlClient = PdlClient(
        config.pdlClientConfig.url,
        "OPP",
        HttpClient()
    ) { azureAdMachineToMachineTokenClient.createMachineToMachineToken(config.pdlClientConfig.scope) }

    val poaoTilgangCachedClient = PoaoTilgangHttpClient(
        config.poaoTilgangClientConfig.url,
        { azureAdMachineToMachineTokenClient.createMachineToMachineToken(config.poaoTilgangClientConfig.scope) }
    )

    val autorisasjonService = AutorisasjonService(poaoTilgangCachedClient)

    val kafkaProducerClient = KafkaProducer<Long, Hendelse>(config.kafka.kafkaProducerProperties)

    val nonBlockingKafkaProducer = NonBlockingKafkaProducer(
        kafkaProducerClient
    )
    val arbeidssokerService = ArbeidssokerService(
        pdlClient = pdlClient,
        nonBlockingKafkaProducer = nonBlockingKafkaProducer,
        topic = config.kafka.producers.arbeidssokerperiodeStartV1.topic
    )

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

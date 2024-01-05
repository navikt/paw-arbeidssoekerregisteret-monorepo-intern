package no.nav.paw.arbeidssokerregisteret

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.config.NaisEnv
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.services.RequestValidator
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.services.RequestHandler
import no.nav.paw.arbeidssokerregisteret.utils.createMockRSAKey
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.pdl.PdlClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.Serializer

fun createDependencies(config: Config, kafkaFactory: KafkaFactory): Dependencies {
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

    val objectMapper = ObjectMapper().registerKotlinModule()
    val kafkaProducerClient = kafkaFactory.createProducer(
        clientId = "paw-arbeidssokerregisteret",
        keySerializer = LongSerializer(),
        valueSerializer = Serializer<Hendelse> { _, data ->
            objectMapper.writeValueAsBytes(data)
        }
    )

    val personInfoService = PersonInfoService(pdlClient)

    val requestValidator = RequestValidator(
        autorisasjonService = autorisasjonService,
        personInfoService = personInfoService,
    )

    return Dependencies(
        registry = registry,
        autorisasjonService = autorisasjonService,
        requestHandler = RequestHandler(
            hendelseTopic = config.eventLogTopic,
            requestValidator = requestValidator,
            producer = kafkaProducerClient
        ),
        kafkaProducer = kafkaProducerClient
    )
}

data class Dependencies(
    val registry: PrometheusMeterRegistry,
    val autorisasjonService: AutorisasjonService,
    val requestHandler: RequestHandler,
    val kafkaProducer: Producer<Long, Hendelse>
)

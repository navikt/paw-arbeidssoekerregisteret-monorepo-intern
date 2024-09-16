package no.nav.paw.arbeidssokerregisteret

import io.ktor.client.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.arbeidssokerregisteret.application.OpplysningerRequestHandler
import no.nav.paw.arbeidssokerregisteret.application.Regler
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.application.RequestValidator
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.utils.azureAdM2MTokenClient
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.paw.pdl.PdlClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import org.apache.kafka.common.serialization.LongSerializer


fun requestHandlers(
    config: Config,
    kafkaFactory: KafkaFactory,
    regler: Regler,
    registry: PrometheusMeterRegistry
): Pair<StartStoppRequestHandler, OpplysningerRequestHandler> {
    val clients = with(azureAdM2MTokenClient(config.naisEnv, config.authProviders.azure)) {
        clientsFactory(config)
    }

    val kafkaProducer = kafkaFactory.createProducer(
        clientId = ApplicationInfo.id,
        keySerializer = LongSerializer::class,
        valueSerializer = HendelseSerializer::class
    )
    val requestValidator = RequestValidator(
        autorisasjonService = AutorisasjonService(clients.poaoTilgangClient),
        personInfoService = PersonInfoService(clients.pdlClient),
        regler = regler,
        registry = registry
    )
    val startStoppRequestHandler = StartStoppRequestHandler(
        hendelseTopic = config.eventLogTopic,
        requestValidator = requestValidator,
        producer = kafkaProducer,
        kafkaKeysClient = clients.kafkaKeysClient
    )

    val opplysningerRequestHandler = OpplysningerRequestHandler(
        hendelseTopic = config.eventLogTopic,
        requestValidator = requestValidator,
        producer = kafkaProducer,
        kafkaKeysClient = clients.kafkaKeysClient
    )

    return startStoppRequestHandler to opplysningerRequestHandler
}

context(AzureAdMachineToMachineTokenClient)
private fun clientsFactory(config: Config): Clients {
    val pdlClient = PdlClient(
        config.pdlClientConfig.url,
        "OPP",
        HttpClient()
    ) { createMachineToMachineToken(config.pdlClientConfig.scope) }
    val poaoTilgangCachedClient = PoaoTilgangHttpClient(
        config.poaoTilgangClientConfig.url,
        { createMachineToMachineToken(config.poaoTilgangClientConfig.scope) }
    )
    val kafkaKeysClient = kafkaKeysClient(config.kafkaKeysConfig) {
        createMachineToMachineToken(config.kafkaKeysConfig.scope)
    }
    return Clients(
        pdlClient = pdlClient,
        kafkaKeysClient = kafkaKeysClient,
        poaoTilgangClient = poaoTilgangCachedClient
    )
}

private data class Clients(
    val pdlClient: PdlClient,
    val kafkaKeysClient: KafkaKeysClient,
    val poaoTilgangClient: PoaoTilgangHttpClient
)

package no.nav.paw.arbeidssokerregisteret

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.arbeidssokerregisteret.application.OpplysningerRequestHandler
import no.nav.paw.arbeidssokerregisteret.application.Regler
import no.nav.paw.arbeidssokerregisteret.application.RequestValidator
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.config.Config
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.utils.azureAdM2MTokenClient
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.kafkaKeysClient
import no.nav.paw.pdl.PdlClient
import no.nav.paw.tilgangskontroll.client.TILGANGSKONTROLL_CLIENT_CONFIG
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import no.nav.paw.tilgangskontroll.client.TilgangskontrollClientConfig
import no.nav.paw.tilgangskontroll.client.tilgangsTjenesteForAnsatte
import org.apache.kafka.common.serialization.LongSerializer


fun requestHandlers(
    config: Config,
    kafkaFactory: KafkaFactory,
    regler: Regler,
    registry: PrometheusMeterRegistry
): Pair<StartStoppRequestHandler, OpplysningerRequestHandler> {
    val clients = with(azureAdM2MTokenClient(config.naisEnv, config.authProviders.azure)) {
        clientsFactory(
            config = config,
            tilgangskontrollClientConfig = loadNaisOrLocalConfiguration(TILGANGSKONTROLL_CLIENT_CONFIG)
        )
    }

    val kafkaProducer = kafkaFactory.createProducer(
        clientId = ApplicationInfo.id,
        keySerializer = LongSerializer::class,
        valueSerializer = HendelseSerializer::class
    )
    val requestValidator = RequestValidator(
        autorisasjonService = AutorisasjonService(clients.tilgangsTjenesteForAnsatte),
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

private fun AzureAdMachineToMachineTokenClient.clientsFactory(
    config: Config,
    tilgangskontrollClientConfig: TilgangskontrollClientConfig
): Clients {
    val pdlClient = PdlClient(
        config.pdlClientConfig.url,
        "OPP",
        HttpClient()
    ) { createMachineToMachineToken(config.pdlClientConfig.scope) }
    val tilgangskontrollClient = tilgangsTjenesteForAnsatte(
        httpClient = HttpClient {
            install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                }
            }
        },
        config = tilgangskontrollClientConfig,
        tokenProvider = { createMachineToMachineToken(tilgangskontrollClientConfig.scope) }
    )
    val kafkaKeysClient = kafkaKeysClient(config.kafkaKeysConfig) {
        createMachineToMachineToken(config.kafkaKeysConfig.scope)
    }
    return Clients(
        pdlClient = pdlClient,
        kafkaKeysClient = kafkaKeysClient,
        tilgangsTjenesteForAnsatte = tilgangskontrollClient
    )
}

private data class Clients(
    val pdlClient: PdlClient,
    val kafkaKeysClient: KafkaKeysClient,
    val tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte
)

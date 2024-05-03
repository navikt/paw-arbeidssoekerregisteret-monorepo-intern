package no.nav.paw.arbeidssokerregisteret.config

const val CONFIG_FILE_NAME = "application.yaml"

data class Config(
    val authProviders: AuthProviders,
    val pdlClientConfig: ServiceClientConfig,
    val poaoTilgangClientConfig: ServiceClientConfig,
    val kafkaKeysConfig: KafkaKeysConfig,
    val eventLogTopic: String,
    val naisEnv: NaisEnv = currentNaisEnv
)

data class AuthProviders(
    val azure: AuthProvider,
    val tokenx: AuthProvider
)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val claims: List<String>
)

data class ServiceClientConfig(
    val url: String,
    val scope: String
)

data class KafkaKeysConfig(
    val url: String,
    val scope: String
)

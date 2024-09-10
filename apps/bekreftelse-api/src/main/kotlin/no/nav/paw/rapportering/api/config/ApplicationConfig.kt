package no.nav.paw.rapportering.api.config

import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentNaisEnv
import java.net.InetAddress

const val APPLICATION_CONFIG_FILE_NAME = "application_config.toml"

data class ApplicationConfig(
    val applicationIdSuffix: String,
    val producerId: String,
    val rapporteringTopic: String,
    val rapporteringHendelseLoggTopic: String,
    val rapporteringStateStoreName: String,
    val authProviders: AuthProviders,
    val naisEnv: NaisEnv = currentNaisEnv,
    val hostname: String = InetAddress.getLocalHost().hostName,
    val poaoClientConfig: ServiceClientConfig
)

data class ServiceClientConfig(
    val url: String,
    val scope: String
)

data class AuthProvider(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String,
    val claims: Claims
)

typealias AuthProviders = List<AuthProvider>

data class Claims(
    val map: List<String>,
    val combineWithOr: Boolean = false
)
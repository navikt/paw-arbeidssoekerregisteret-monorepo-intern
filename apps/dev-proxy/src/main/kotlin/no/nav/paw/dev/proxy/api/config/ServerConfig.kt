package no.nav.paw.dev.proxy.api.config

import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment

const val SERVER_CONFIG = "server_config.toml"

data class ServerConfig(
    val host: String,
    val port: Int,
    val callGroupSize: Int,
    val workerGroupSize: Int,
    val connectionGroupSize: Int,
    val gracePeriodMillis: Long,
    val timeoutMillis: Long,
    val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
)

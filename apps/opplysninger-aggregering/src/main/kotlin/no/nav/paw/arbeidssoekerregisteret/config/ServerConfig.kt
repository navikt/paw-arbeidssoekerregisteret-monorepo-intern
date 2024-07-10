package no.nav.paw.arbeidssoekerregisteret.config

const val SERVER_CONFIG_FILE_NAME = "server_configuration.toml"

data class ServerConfig(
    val port: Int,
    val callGroupSize: Int,
    val workerGroupSize: Int,
    val connectionGroupSize: Int,
    val gracePeriodMillis: Long,
    val timeoutMillis: Long
)

package no.nav.paw.arbeidssokerregisteret.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource
import no.nav.paw.config.kafka.KAFKA_CONFIG

inline fun <reified T : Any> loadConfiguration(): T = ConfigLoaderBuilder.default()
    .apply {
        when (currentNaisEnv) {
            NaisEnv.ProdGCP -> {
                addResourceSource("/application-prod.yaml", optional = true)
            }

            NaisEnv.DevGCP -> {
                addResourceSource("/application-dev.yaml", optional = true)
            }

            NaisEnv.Local -> {
                addResourceSource("/application-local.yaml", optional = true)
            }
        }
    }
    .strict()
    .build()
    .loadConfigOrThrow()

inline fun <reified T : Any> loadKafkaConfiguration(): T = ConfigLoaderBuilder.default()
    .apply {
        when (currentNaisEnv) {
            NaisEnv.ProdGCP -> {
                addResourceSource("/nais/kafka_configuration.toml", optional = true)
            }

            NaisEnv.DevGCP -> {
                addResourceSource("/nais/kafka_configuration.toml", optional = true)
            }

            NaisEnv.Local -> {
                addResourceSource("/local/kafka_configuration.toml", optional = true)
            }
        }
    }
    .strict()
    .build()
    .loadConfigOrThrow()


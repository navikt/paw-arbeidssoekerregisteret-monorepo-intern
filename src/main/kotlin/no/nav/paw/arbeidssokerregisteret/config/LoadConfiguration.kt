package no.nav.paw.arbeidssokerregisteret.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource

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


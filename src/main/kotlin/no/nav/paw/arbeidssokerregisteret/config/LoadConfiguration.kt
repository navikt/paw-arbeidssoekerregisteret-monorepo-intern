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

@OptIn(ExperimentalHoplite::class)
inline fun <reified A> lastKonfigurasjon(navn: String): A {
    val fulltNavn = when (System.getenv("NAIS_CLUSTER_NAME")) {
        "prod-gcp" -> "/nais/$navn"
        "dev-gcp" -> "/nais/$navn"
        else -> "/local/$navn"
    }
    return ConfigLoaderBuilder
        .default()
        .withExplicitSealedTypes()
        .addResourceSource(fulltNavn)
        .build()
        .loadConfigOrThrow()
}

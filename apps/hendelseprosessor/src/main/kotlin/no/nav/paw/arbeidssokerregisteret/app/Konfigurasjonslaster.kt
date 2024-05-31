package no.nav.paw.arbeidssokerregisteret.app

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource

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
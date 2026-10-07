package no.nav.paw.config.hoplite

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource

/**
 * Laster config basert på gjeldene NAIS miljø.
 * Forutsetter at config ligger i resources under /nais eller /local
 */
inline fun <reified A> loadNaisOrLocalConfiguration(resource: String): A {
    val fulltNavn =
        when (System.getenv("NAIS_CLUSTER_NAME")) {
            "prod-gcp" -> "/nais/$resource"
            "dev-gcp" -> "/nais/$resource"
            else -> "/local/$resource"
        }
    return loadConfigFromProvidedResource(fulltNavn)
}

/**
 * Laster config fra resources
 */
@OptIn(ExperimentalHoplite::class)
inline fun <reified A> loadConfigFromProvidedResource(resource: String): A {
    // Uten property sources: Hoplite 3 lar ellers miljøvariabler (f.eks. Nais sin PORT)
    // overstyre config-nøkler med samme navn. Miljøvariabler hentes bare eksplisitt med ${VAR}.
    return ConfigLoaderBuilder
        .defaultWithoutPropertySources()
        .strict()
        .withExplicitSealedTypes()
        .addResourceSource(resource)
        .build()
        .loadConfigOrThrow()
}

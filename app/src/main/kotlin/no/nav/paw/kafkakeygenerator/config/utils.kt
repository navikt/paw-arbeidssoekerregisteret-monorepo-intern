package no.nav.paw.kafkakeygenerator.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource
import no.nav.paw.kafkakeygenerator.config.NaisEnv.*
import java.lang.System.getenv

@OptIn(ExperimentalHoplite::class)
inline fun <reified A> lastKonfigurasjon(navn: String): A {
    val fulltNavn = when (currentNaisEnv) {
        Local -> "/local/$navn"
        DevGCP -> "/dev/$navn"
        ProdGCP -> "/prod/$navn"
    }
    return ConfigLoaderBuilder
        .default()
        .withExplicitSealedTypes()
        .addResourceSource(fulltNavn)
        .build()
        .loadConfigOrThrow()
}

enum class NaisEnv(val clusterName: String) {
    Local("local"),
    DevGCP("dev-gcp"),
    ProdGCP("prod-gcp");
}

val currentNaisEnv: NaisEnv
    get() =
        when (getenv("NAIS_CLUSTER_NAME")) {
            DevGCP.clusterName -> DevGCP
            ProdGCP.clusterName -> ProdGCP
            else -> Local
        }
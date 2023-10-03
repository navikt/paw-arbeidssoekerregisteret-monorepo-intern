package no.nav.paw.arbeidssokerregisteret.app.config.nais

import no.nav.paw.arbeidssokerregisteret.app.config.nais.NaisEnv.*
import java.lang.System.getenv

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
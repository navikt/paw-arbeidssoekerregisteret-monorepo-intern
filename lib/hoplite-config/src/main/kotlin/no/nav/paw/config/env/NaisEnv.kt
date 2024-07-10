package no.nav.paw.config.env

enum class NaisEnv(val clusterName: String) {
    Local("local"),
    DevGCP("dev-gcp"),
    ProdGCP("prod-gcp")
}

val currentNaisEnv: NaisEnv
    get() =
        when (System.getenv("NAIS_CLUSTER_NAME")) {
            NaisEnv.DevGCP.clusterName -> NaisEnv.DevGCP
            NaisEnv.ProdGCP.clusterName -> NaisEnv.ProdGCP
            else -> NaisEnv.Local
        }

val currentAppId: String? get() = System.getenv("NAIS_APP_IMAGE") // F.eks. europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssoekerregisteret-utgang-pdl:24.01.01.01-1

val currentAppName: String? get() = System.getenv("NAIS_APP_NAME") // F.eks. paw-arbeidssoekerregisteret-utgang-pdl

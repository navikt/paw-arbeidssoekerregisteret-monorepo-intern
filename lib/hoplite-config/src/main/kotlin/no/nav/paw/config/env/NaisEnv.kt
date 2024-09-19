package no.nav.paw.config.env

enum class NaisEnv(
    val clusterName: String,
    val namespace: String?,
    val appName: String?
) {
    Local("local", "local-namespace", "local-app"),
    DevGCP(
        clusterName = "dev-gcp",
        namespace = currentNamespace,
        appName = currentAppName
    ),
    ProdGCP(
        clusterName = "prod-gcp",
        namespace = currentNamespace,
        appName = currentAppName
    )
}

val currentNaisEnv: NaisEnv
    get() =
        when (System.getenv("NAIS_CLUSTER_NAME")) {
            NaisEnv.DevGCP.clusterName -> NaisEnv.DevGCP
            NaisEnv.ProdGCP.clusterName -> NaisEnv.ProdGCP
            else -> NaisEnv.Local
        }

val currentAppId: String? get() = System.getenv("NAIS_APP_IMAGE") // F.eks. europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-microfrontend-toggler:24.06.27.57-1

val currentAppName: String? get() = System.getenv("NAIS_APP_NAME") // F.eks. paw-microfrontend-toggler

val currentNamespace: String? get() = System.getenv("NAIS_NAMESPACE")
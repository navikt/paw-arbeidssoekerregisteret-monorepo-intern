package no.nav.paw.config.env

sealed interface RuntimeEnvironment

data object Local : RuntimeEnvironment

sealed interface Nais : RuntimeEnvironment {
    val clusterName: String
    val namespace: String
    val appName: String
    val appImage: String
}
sealed interface DevGcp : Nais

sealed interface ProdGcp : Nais


private class DevGCPImpl(
    override val clusterName: String,
    override val namespace: String,
    override val appName: String,
    override val appImage: String
) : DevGcp

private class ProdGCPImpl(
    override val clusterName: String,
    override val namespace: String,
    override val appName: String,
    override val appImage: String
) : ProdGcp

const val NAIS_PROD_CLUSER_NAME = "prod-gcp"
const val NAIS_DEV_CLUSER_NAME = "dev-gcp"

val currentRuntimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment()

private fun currentRuntimeEnvironment(): RuntimeEnvironment {
    val namespace = { currentNamespace ?: error("NAIS_NAMESPACE is not set") }
    val appName = { currentAppName ?: error("NAIS_APP_NAME is not set") }
    val appImage = { currentAppImage ?: error("NAIS_APP_IMAGE is not set") }
    return when (val clusterName = System.getenv("NAIS_CLUSTER_NAME")) {
        NAIS_PROD_CLUSER_NAME -> ProdGCPImpl(clusterName, namespace(), appName(), appImage())
        NAIS_DEV_CLUSER_NAME -> DevGCPImpl(clusterName, namespace(), appName(), appImage())
        else -> Local
    }
}

private val currentAppImage: String? get() = System.getenv("NAIS_APP_IMAGE") // F.eks. europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-microfrontend-toggler:24.06.27.57-1

private val currentAppName: String? get() = System.getenv("NAIS_APP_NAME") // F.eks. paw-microfrontend-toggler

private val currentNamespace: String? get() = System.getenv("NAIS_NAMESPACE")
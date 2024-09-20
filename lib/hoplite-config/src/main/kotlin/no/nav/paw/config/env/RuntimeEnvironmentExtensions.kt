package no.nav.paw.config.env

fun RuntimeEnvironment.namespaceOrDefaultForLocal(default: String = "local-namespace") = when (this) {
    is Nais -> namespace
    else -> default
}

fun RuntimeEnvironment.clusterNameOrDefaultForLocal(default: String = "local") = when (this) {
    is Nais -> clusterName
    else -> default
}

fun RuntimeEnvironment.appNameOrDefaultForLocal(default: String = "local-app") = when (this) {
    is Nais -> appName
    else -> default
}

fun RuntimeEnvironment.appImageOrDefaultForLocal(default: String = "local-image") = when (this) {
    is Nais -> appImage
    else -> default
}
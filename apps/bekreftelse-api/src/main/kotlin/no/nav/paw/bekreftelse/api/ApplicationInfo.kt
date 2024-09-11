package no.nav.paw.bekreftelse.api

object ApplicationInfo {
    val id = System.getenv("IMAGE_WITH_VERSION") ?: "UNSPECIFIED"
}
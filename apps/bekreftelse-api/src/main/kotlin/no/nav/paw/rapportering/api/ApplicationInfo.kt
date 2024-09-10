package no.nav.paw.rapportering.api

object ApplicationInfo {
    val id = System.getenv("IMAGE_WITH_VERSION") ?: "UNSPECIFIED"
}
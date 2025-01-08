package no.nav.paw.tilgangskontroll.client

import java.net.URI

const val TILGANGSKONTROLL_CLIENT_CONFIG = "tilgangskontroll_client_config.toml"

data class TilgangskontrollClientConfig(
    val uri: String,
    val scope: String
)

fun TilgangskontrollClientConfig.apiTilgangV1(): URI = URI.create(uri).resolve("/api/v1/tilgang")
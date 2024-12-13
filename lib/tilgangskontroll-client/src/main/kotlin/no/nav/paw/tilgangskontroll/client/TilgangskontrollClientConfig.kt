package no.nav.paw.tilgangskontroll.client

import java.net.URI

class TilgangskontrollClientConfig(
    val uri: String,
    val scope: String
)

fun TilgangskontrollClientConfig.apiTilgangV1(): URI = URI.create(uri).resolve("/api/v1/tilgang")
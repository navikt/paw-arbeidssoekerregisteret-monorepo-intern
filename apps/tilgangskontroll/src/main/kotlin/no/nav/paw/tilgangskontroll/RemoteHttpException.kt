package no.nav.paw.tilgangskontroll

import java.io.IOException

class RemoteHttpException(
    msg: String,
    val statusCode: Int,
    val statusMessage: String
): IOException(msg)
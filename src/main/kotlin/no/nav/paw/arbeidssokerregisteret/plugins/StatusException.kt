package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.http.HttpStatusCode

open class StatusException(val status: HttpStatusCode, val description: String? = null) :
    Exception("Request failed with status: $status. Description: $description")

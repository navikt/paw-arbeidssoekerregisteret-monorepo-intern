package no.nav.paw.dev.proxy.api.model

import com.fasterxml.jackson.databind.JsonNode

data class ProxyRequest(
    val method: String,
    val url: String,
    val body: JsonNode? = null,
)

package no.nav.paw.dev.proxy.api.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import no.nav.paw.dev.proxy.api.model.ProxyRequest
import no.nav.paw.dev.proxy.api.model.ProxyResponse
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.serialization.jackson.buildObjectMapper

private val logger = buildApplicationLogger
private val objectMapper = buildObjectMapper

class ProxyService(private val httpClient: HttpClient) {

    suspend fun proxy(
        requestHeaders: Headers,
        proxyRequest: ProxyRequest
    ): ProxyResponse {
        logger.debug("Sending proxy request")
        val clientResponse = httpClient.request {
            method = HttpMethod.parse(proxyRequest.method)
            url.takeFrom(proxyRequest.url)
            headers.appendAll(requestHeaders)
            setBody(proxyRequest.body)
        }
        val contentType = clientResponse.contentType()
        val body: JsonNode? = clientResponse.body<String>()
            .let {
                if (it.isBlank()) {
                    logger.info("Received proxy response, status: {}, no body", clientResponse.status)
                    null
                } else if (contentType != null && contentType == ContentType.Application.Json) {
                    logger.info("Received proxy response, status: {}, body:\n{}", clientResponse.status, it)
                    objectMapper.readTree(it)
                } else {
                    TextNode(it)
                }
            }
        logger.debug("Proxy response headers: {}", clientResponse.headers)
        return ProxyResponse(
            method = proxyRequest.method,
            url = proxyRequest.url,
            status = clientResponse.status,
            contentType = contentType,
            headers = clientResponse.headers,
            body = body
        )
    }
}
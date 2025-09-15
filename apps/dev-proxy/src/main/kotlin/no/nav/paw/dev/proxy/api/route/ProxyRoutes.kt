package no.nav.paw.dev.proxy.api.route

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.paw.dev.proxy.api.model.ProxyRequest
import no.nav.paw.dev.proxy.api.service.ProxyService

fun Route.proxyRoutes(proxyService: ProxyService) {
    route("/api/v1") {
        post<ProxyRequest>("/proxy") { proxyRequest ->
            val proxyResponse = proxyService.proxy(
                requestHeaders = call.request.headers,
                proxyRequest = proxyRequest
            )
            call.respond(proxyResponse.status, proxyResponse)
        }
    }
}

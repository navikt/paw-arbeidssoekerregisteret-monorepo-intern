package no.nav.paw.rapportering.api.routes

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

fun Route.swaggerRoutes() {
    swaggerUI(path = "docs", swaggerFile = "openapi/rapporteringapi.yaml")
}
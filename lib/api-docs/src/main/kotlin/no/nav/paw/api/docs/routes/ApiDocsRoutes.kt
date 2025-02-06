package no.nav.paw.api.docs.routes

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

fun Route.apiDocsRoutes(
    path: String = "docs",
    swaggerFile: String = "openapi/documentation.yaml"
) {
    swaggerUI(path, swaggerFile)
}

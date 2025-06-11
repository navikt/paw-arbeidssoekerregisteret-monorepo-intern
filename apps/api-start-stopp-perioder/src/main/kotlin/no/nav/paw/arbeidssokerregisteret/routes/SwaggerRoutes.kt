package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Route

fun Route.swaggerRoutes() {
    swaggerUI(path = "docs/startstopp", swaggerFile = "openapi/startstopp.yaml")
    swaggerUI(path = "docs/opplysninger", swaggerFile = "openapi/opplysninger.yaml")
}

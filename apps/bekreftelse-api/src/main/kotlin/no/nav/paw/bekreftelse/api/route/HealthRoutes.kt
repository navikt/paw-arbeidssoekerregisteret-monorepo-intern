package no.nav.paw.bekreftelse.api.route

import io.ktor.server.routing.Route
import no.nav.paw.health.model.LivenessCheck
import no.nav.paw.health.model.ReadinessCheck
import no.nav.paw.health.route.livenessRoute
import no.nav.paw.health.route.readinessRoute

fun Route.healthRoutes(
    readinessChecks: Collection<ReadinessCheck>,
    livenessChecks: Collection<LivenessCheck>
) {
    readinessRoute(readinessChecks = readinessChecks.toTypedArray())
    livenessRoute(livenessChecks = livenessChecks.toTypedArray())
}
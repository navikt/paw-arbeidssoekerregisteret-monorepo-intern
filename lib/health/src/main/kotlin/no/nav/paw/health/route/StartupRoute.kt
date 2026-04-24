package no.nav.paw.health.route

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.StartupCheck

const val startupPath = "/internal/isStarted"

fun Route.startupRoute(vararg startupChecks: StartupCheck) {
    get(startupPath) {
        val startupComplete = startupChecks.all { startupCheck -> startupCheck.isStarted() }
        when (startupComplete) {
            true -> call.respondText(
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.OK
            ) { HealthStatus.HEALTHY.value }

            false -> call.respondText(
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.ServiceUnavailable
            ) { HealthStatus.UNHEALTHY.value }
        }
    }
}
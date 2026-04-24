package no.nav.paw.health.route

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessCheck

const val livenessPath = "/internal/isAlive"

fun Route.livenessRoute(vararg livenessChecks: LivenessCheck) {
    get(livenessPath) {
        val applicationAlive = livenessChecks.all { livenessCheck -> livenessCheck.isAlive() }
        when (applicationAlive) {
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
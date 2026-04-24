package no.nav.paw.health.route

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.ReadinessCheck

const val readinessPath = "/internal/isReady"

fun Route.readinessRoute(vararg readinessChecks: ReadinessCheck) {
    get(readinessPath) {
        val applicationReady = readinessChecks.all { readinessCheck -> readinessCheck.isReady() }
        when (applicationReady) {
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
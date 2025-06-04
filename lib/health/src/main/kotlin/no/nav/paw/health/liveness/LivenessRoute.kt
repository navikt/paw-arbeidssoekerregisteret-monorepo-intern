package no.nav.paw.health.liveness

import io.ktor.http.ContentType.Text
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.health.HealthStatus.HEALTHY
import no.nav.paw.health.HealthStatus.UNHEALTHY

const val livenessPath = "/internal/isAlive"

fun interface LivenessCheck {
    fun isAlive(): Boolean
}

fun Route.livenessRoute(vararg livenessChecks: LivenessCheck) {
    get(livenessPath) {
        val applicationAlive = livenessChecks.all { livenessCheck -> livenessCheck.isAlive() }
        when (applicationAlive) {
            true -> call.respondText(contentType = Text.Plain, status = OK) { HEALTHY.value }
            false -> call.respondText(contentType = Text.Plain, status = ServiceUnavailable) { UNHEALTHY.value }
        }
    }
}
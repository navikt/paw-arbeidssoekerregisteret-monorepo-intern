package no.nav.paw.arbeidssoekerregisteret.backup.health

import io.ktor.http.ContentType.Text
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.health.model.HealthStatus.HEALTHY
import no.nav.paw.health.model.HealthStatus.UNHEALTHY

const val startupPath = "/internal/startup"

fun interface StartupProbe {
    fun isReady(): Boolean
}

fun Route.startupRoute(vararg startupChecks: StartupProbe) {
    get(startupPath) {
        val startupComplete = startupChecks.all { startupCheck -> startupCheck.isReady() }
        when (startupComplete) {
            true -> call.respondText(contentType = Text.Plain, status = OK) { HEALTHY.value }
            false -> call.respondText(contentType = Text.Plain, status = ServiceUnavailable) { UNHEALTHY.value }
        }
    }
}

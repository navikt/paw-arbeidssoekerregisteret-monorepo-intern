package no.nav.paw.error.plugin

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import no.nav.paw.error.handler.handleException
import no.nav.paw.error.model.ProblemDetails

const val ERROR_HANDLING_PLUGIN_NAME: String = "ErrorHandlingPlugin"

class ErrorHandlingPluginConfig {
    val resolveProblemDetails: ((Throwable) -> ProblemDetails?)? = null
}

val ErrorHandlingPlugin
    get(): RouteScopedPlugin<ErrorHandlingPluginConfig> = createRouteScopedPlugin(
        ERROR_HANDLING_PLUGIN_NAME,
        ::ErrorHandlingPluginConfig
    ) {
        application.log.info("Installerer {}", ERROR_HANDLING_PLUGIN_NAME)
        val resolveProblemDetails = pluginConfig.resolveProblemDetails ?: { null }

        application.install(StatusPages) {
            exception<Throwable> { call: ApplicationCall, cause: Throwable ->
                call.handleException(cause, resolveProblemDetails)
            }
        }
    }

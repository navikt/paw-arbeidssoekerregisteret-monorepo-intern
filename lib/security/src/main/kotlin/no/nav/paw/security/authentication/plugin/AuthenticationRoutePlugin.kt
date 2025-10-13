package no.nav.paw.security.authentication.plugin

import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.log
import io.ktor.server.auth.AuthenticationChecked
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.resolveSecurityContext
import no.nav.paw.security.authentication.model.securityContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authentication")

private data object PluginName : PluginInstanceName("AuthenticationPlugin")

class AuthenticationRoutePluginConfig {
    var modifyPrincipal: (suspend (SecurityContext) -> SecurityContext)? = null
}

val AuthenticationRoutePlugin
    get(): RouteScopedPlugin<AuthenticationRoutePluginConfig> = createRouteScopedPlugin(
        PluginName.pluginInstanceName,
        ::AuthenticationRoutePluginConfig
    ) {
        application.log.info(
            "Installerer {}{}",
            PluginName.pluginName,
            PluginName.pluginInstance
        )
        val modifyPrincipal = pluginConfig.modifyPrincipal ?: { it }

        on(AuthenticationChecked) { call ->
            logger.trace("Kjører autentisering")
            val securityContext = modifyPrincipal(call.resolveSecurityContext())
            call.securityContext(securityContext)
        }
    }

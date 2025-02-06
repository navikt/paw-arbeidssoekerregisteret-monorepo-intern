package no.nav.paw.security.authentication.plugin

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import no.nav.paw.security.authentication.model.Issuer
import no.nav.paw.security.authentication.model.SecurityContext

fun Route.autentisering(
    issuer: Issuer,
    modifyPrincipal: (suspend (SecurityContext) -> SecurityContext)? = null,
    build: Route.() -> Unit
): Route {
    return autentisering(issuers = arrayOf(issuer), modifyPrincipal = modifyPrincipal, build = build)
}

fun Route.autentisering(
    vararg issuers: Issuer = emptyArray(),
    modifyPrincipal: (suspend (SecurityContext) -> SecurityContext)? = null,
    build: Route.() -> Unit
): Route {
    install(AuthenticationRoutePlugin) {
        this.modifyPrincipal = modifyPrincipal
    }
    val configurations: Array<String> = issuers.map { issuer -> issuer.name }.toTypedArray()
    return authenticate(*configurations, optional = false, build = build)
}

package no.nav.paw.arbeidssoekerregisteret.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.exception.IkkeTilgjengeligException
import no.nav.paw.arbeidssoekerregisteret.service.BestillingService
import no.nav.paw.arbeidssoekerregisteret.utils.pathBestillingId
import no.nav.paw.security.authentication.model.AzureAd
import no.nav.paw.security.authentication.model.Bruker
import no.nav.paw.security.authentication.model.bruker
import no.nav.paw.security.authentication.plugin.autentisering

fun Route.bestillingerRoutes(
    applicationConfig: ApplicationConfig,
    bestillingService: BestillingService
) {
    route("/api/v1/bestillinger") {
        autentisering(AzureAd) {
            get("/{bestillingId}") {
                val bestillingId = call.pathBestillingId()
                val response = bestillingService.hentBestilling(bestillingId)
                call.respond(HttpStatusCode.OK, response)
            }

            post {
                if (applicationConfig.manueltVarselEnabled) {
                    val bruker = call.bruker<Bruker<String>>()
                    val response = bestillingService.opprettBestilling(bruker.ident)
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    throw IkkeTilgjengeligException("Funksjonen er utilgjengelig for øyeblikket")
                }
            }

            put("/{bestillingId}") {
                if (applicationConfig.manueltVarselEnabled) {
                    val bestillingId = call.pathBestillingId()
                    val response = bestillingService.bekreftBestilling(bestillingId)
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    throw IkkeTilgjengeligException("Funksjonen er utilgjengelig for øyeblikket")
                }
            }
        }
    }
}
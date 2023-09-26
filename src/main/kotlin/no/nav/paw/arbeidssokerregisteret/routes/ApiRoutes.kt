package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.paw.arbeidssokerregisteret.arbeidssoker.ArbeidssokerRepository
import no.nav.paw.arbeidssokerregisteret.arbeidssoker.ArbeidssokerService
import no.nav.paw.arbeidssokerregisteret.arbeidssoker.arbeidssokerRoutes
import org.jetbrains.exposed.sql.Database

fun Route.apiRoutes(database: Database) {
    val arbeidssokerRepository = ArbeidssokerRepository(database)
    val arbeidssokerService = ArbeidssokerService(arbeidssokerRepository)

    route("/api/v1") {
        arbeidssokerRoutes(arbeidssokerService)
    }
}

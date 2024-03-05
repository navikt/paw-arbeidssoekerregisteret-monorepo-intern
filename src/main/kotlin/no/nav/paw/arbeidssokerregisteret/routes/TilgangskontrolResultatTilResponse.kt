package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import no.nav.paw.arbeidssokerregisteret.application.EksternRegelId
import no.nav.paw.arbeidssokerregisteret.application.IkkeTilgang
import no.nav.paw.arbeidssokerregisteret.application.TilgangOK
import no.nav.paw.arbeidssokerregisteret.application.TilgangskontrollResultat
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode
import no.nav.paw.arbeidssokerregisteret.domain.http.AarsakTilAvvisning
import no.nav.paw.arbeidssokerregisteret.domain.http.Feil


context(PipelineContext<Unit, ApplicationCall>)
suspend fun respondWith(resultat: TilgangskontrollResultat) =
    when (resultat) {
        is IkkeTilgang -> call.respond(
            HttpStatusCode.Forbidden, Feil(
                melding = resultat.regel.beskrivelse,
                feilKode = Feilkode.AVVIST,
                aarsakTilAvvisning = AarsakTilAvvisning(
                    beskrivelse = resultat.regel.beskrivelse,
                    regel = resultat.regel.id.eksternRegelId ?: EksternRegelId.UKJENT,
                    detaljer = resultat.regel.opplysninger.toSet()
                )
            )
        )
        is TilgangOK -> call.respond(HttpStatusCode.OK)
    }

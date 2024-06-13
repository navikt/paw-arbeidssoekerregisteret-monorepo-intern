package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode
import no.nav.paw.arbeidssokerregisteret.domain.http.AarsakTilAvvisning
import no.nav.paw.arbeidssokerregisteret.domain.http.Feil

context(PipelineContext<Unit, ApplicationCall>)
@WithSpan
suspend fun respondWith(resultat: EndeligResultat) {
    val otelStatusCode = if (resultat is OK) StatusCode.OK else StatusCode.ERROR
    Span.current()
        .setStatus(otelStatusCode, resultat::class.simpleName!!)
    when (resultat) {
        is Avvist -> call.respond(
            HttpStatusCode.Forbidden, Feil(
                melding = resultat.regel.beskrivelse,
                feilKode = Feilkode.AVVIST,
                aarsakTilAvvisning = AarsakTilAvvisning(
                    beskrivelse = resultat.regel.beskrivelse,
                    regel = resultat.regel.id.eksternRegelId ?: EksternRegelId.UKJENT_REGEL,
                    detaljer = resultat.opplysning.toSet()
                )
            )
        )

        is IkkeTilgang -> call.respond(
            HttpStatusCode.Forbidden, Feil(
                melding = resultat.regel.beskrivelse,
                feilKode = Feilkode.IKKE_TILGANG
            )
        )

        is OK -> call.respond(HttpStatusCode.NoContent)
        is Uavklart -> call.respond(
            HttpStatusCode.Forbidden, Feil(
                melding = resultat.regel.beskrivelse,
                feilKode = Feilkode.AVVIST
            )
        )

        is UgyldigRequestBasertPaaAutentisering -> call.respond(
            HttpStatusCode.BadRequest, Feil(
                melding = resultat.regel.beskrivelse,
                feilKode = Feilkode.UGYLDIG_FORESPORSEL
            )
        )
    }
}

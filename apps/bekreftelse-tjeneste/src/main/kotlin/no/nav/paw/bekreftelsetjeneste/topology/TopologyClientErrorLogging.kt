package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import org.slf4j.LoggerFactory

private val clientErrorLogger = LoggerFactory.getLogger("client_error")

fun errorLog(
    loesning: Loesning,
    handling: String,
    feil: Feil,
    harAnsvar: Boolean = feil.harAnsvar,
    tilstand: String? = null
) {
    with(Span.current()) {
        addEvent(
            errorEvent, Attributes.builder()
                .put(domainKey, "bekreftelse")
                .put(actionKey, handling)
                .put(bekreftelseloesingKey, loesning.name)
                .put(harAnsvarKey, harAnsvar)
                .put(periodeFunnetKey, feil.periodeFunnet)
                .put(feilMeldingKey, feil.melding)
                .let { if (tilstand != null) it.put(tilstandKey, tilstand) else it }
                .build()
        )
        setStatus(StatusCode.ERROR, feil.melding)
    }
    clientErrorLogger.error(
        "loesning={}, handling={}, Feil={}, melding={} har_ansvar={}, periode_funnet={}, tilstand={}",
        loesning,
        handling,
        feil.name.lowercase(),
        feil.melding,
        harAnsvar,
        feil.periodeFunnet,
        tilstand ?: "NA"
    )
}

enum class Feil(val periodeFunnet: Boolean, val harAnsvar: Boolean, val melding: String) {
    HAR_IKKE_ANSVAR(periodeFunnet = true, harAnsvar = false, "har ikke ansvar"),
    PERIODE_IKKE_FUNNET(periodeFunnet = false, harAnsvar = false, "periode ikke funnet"),
    BEKREFTELSE_IKKE_FUNNET(periodeFunnet = true, harAnsvar = true, "bekreftelse ikke funnet"),
    UVENTET_TILSTAND(periodeFunnet = true, harAnsvar = true, "Bekreftelse har ikke forventet tilstand")
}

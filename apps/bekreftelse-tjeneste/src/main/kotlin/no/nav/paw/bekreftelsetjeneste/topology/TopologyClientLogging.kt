package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandStatus
import no.nav.paw.bekreftelsetjeneste.tilstand.tilstand
import org.slf4j.LoggerFactory

private val clientLogger = LoggerFactory.getLogger("bekreftelse.tjeneste.client")

fun attributes(
    loesning: Loesning,
    handling: String,
    periodeFunnet: Boolean,
    harAnsvar: Boolean,
    feilMelding: String? = null,
    tilstand: String? = null
): Attributes = Attributes.builder()
    .put(domainKey, "bekreftelse")
    .put(actionKey, handling)
    .put(bekreftelseloesingKey, loesning.name)
    .put(harAnsvarKey, harAnsvar)
    .put(periodeFunnetKey, periodeFunnet)
    .let { if (feilMelding != null) it.put(feilMeldingKey, feilMelding) else it }
    .let { if (tilstand != null) it.put(tilstandKey, tilstand) else it }
    .build()

fun log(
    loesning: Loesning,
    handling: String,
    periodeFunnet: Boolean,
    harAnsvar: Boolean,
    tilstand: String? = null
) {
    val attributes = attributes(loesning, handling, periodeFunnet, harAnsvar, tilstand = tilstand)
    with(Span.current()) {
        setAllAttributes(attributes)
        addEvent(okEvent, attributes)
        setStatus(StatusCode.OK)
    }
    clientLogger.trace(
        "OK, {}",
        attributes.asMap().mapKeys { it.key.key }
    )
}

fun logWarning(
    loesning: Loesning,
    handling: String,
    feil: Feil,
    harAnsvar: Boolean = feil.harAnsvar,
    tilstand: BekreftelseTilstandStatus? = null
) {
    val attributes = attributes(
        loesning = loesning,
        handling = handling,
        periodeFunnet = feil.periodeFunnet,
        harAnsvar = harAnsvar,
        feilMelding = feil.name,
        tilstand = tilstand?.javaClass?.simpleName?.map { char ->  if (char.isUpperCase()) "_${char.lowercase()}" else "$char" }
            ?.joinToString("")
    )
    with(Span.current()) {
        setAllAttributes(attributes)
        addEvent(errorEvent, attributes)
        setStatus(StatusCode.ERROR, feil.name)
    }
    clientLogger.warn(
        "Uventet hendelse: {}",
        attributes.asMap().mapKeys { it.key.key }
    )
}

enum class Feil(val periodeFunnet: Boolean, val harAnsvar: Boolean) {
    HAR_IKKE_ANSVAR(periodeFunnet = true, harAnsvar = false),
    PERIODE_IKKE_FUNNET(periodeFunnet = false, harAnsvar = false),
    BEKREFTELSE_IKKE_FUNNET(periodeFunnet = true, harAnsvar = true),
    UVENTET_TILSTAND(periodeFunnet = true, harAnsvar = true)
}

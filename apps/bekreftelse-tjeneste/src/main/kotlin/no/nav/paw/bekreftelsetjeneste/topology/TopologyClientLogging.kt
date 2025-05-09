package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandStatus
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

private val clientLogger = LoggerFactory.getLogger("bekreftelse.tjeneste.client")

private fun attributes(
    loesning: Loesning,
    handling: String,
    periodeFunnet: Boolean,
    harAnsvar: Boolean,
    feilMelding: String? = null,
    tilstand: String? = null,
    fristBrutt: Boolean? = null,
    sistLevert: Instant? = null,
    tidSidenSisteLevering: Duration? = null,
    fristKanVaereBrutt: Boolean? = null,
    utgaaende: List<String>? = null
): Attributes = Attributes.builder()
    .put(domainKey, "bekreftelse")
    .put(actionKey, handling)
    .put(bekreftelseloesingKey, loesning.name)
    .put(harAnsvarKey, harAnsvar)
    .put(periodeFunnetKey, periodeFunnet)
    .let { if (fristBrutt != null) it.put(fristBruttKey, fristBrutt) else it }
    .let { if (feilMelding != null) it.put(feilMeldingKey, feilMelding) else it }
    .let { if (tilstand != null) it.put(tilstandKey, tilstand) else it }
    .let { if (sistLevert != null) it.put(sistLevertKey, sistLevert.toString()) else it }
    .let { if (tidSidenSisteLevering != null) it.put(dagerSidenSisteLeveringKey, tidSidenSisteLevering.toDays()) else it }
    .let { if (fristKanVaereBrutt != null) it.put(fristKanVaereBruttKey, fristKanVaereBrutt) else it }
    .let { if (utgaaende != null) it.put(utgaaendeKey, utgaaende.joinToString(",")) else it }
    .build()

fun log(
    loesning: Loesning,
    handling: String,
    periodeFunnet: Boolean,
    harAnsvar: Boolean,
    tilstand: BekreftelseTilstandStatus? = null,
    fristBrutt: Boolean? = null,
    sistLevert: Instant? = null,
    tidSidenSisteLevering: Duration? = null,
    fristKanVaereBrutt: Boolean? = null,
    utgaaende: List<String>? = null
) {
    val attributes = attributes(
        loesning = loesning,
        handling = handling,
        periodeFunnet = periodeFunnet,
        harAnsvar = harAnsvar,
        tilstand = formaterClassSimpleName(tilstand),
        fristBrutt = fristBrutt,
        sistLevert = sistLevert,
        tidSidenSisteLevering = tidSidenSisteLevering,
        fristKanVaereBrutt = fristKanVaereBrutt,
        utgaaende = utgaaende
    )
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
    tilstand: BekreftelseTilstandStatus? = null,
    fristBrutt: Boolean? = null,
) {
    val attributes = attributes(
        loesning = loesning,
        handling = handling,
        periodeFunnet = feil.periodeFunnet,
        harAnsvar = harAnsvar,
        feilMelding = feil.name,
        tilstand = formaterClassSimpleName(tilstand),
        fristBrutt = fristBrutt,
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

private fun formaterClassSimpleName(tilstand: BekreftelseTilstandStatus?) =
    tilstand?.javaClass?.simpleName
        ?.let { name ->
            "${name.first().lowercase()}${
                name.drop(1).map { char -> if (char.isUpperCase()) "_${char.lowercase()}" else "$char" }
                    .joinToString("")
            }"
        }

enum class Feil(val periodeFunnet: Boolean, val harAnsvar: Boolean) {
    HAR_IKKE_ANSVAR(periodeFunnet = true, harAnsvar = false),
    PERIODE_IKKE_FUNNET(periodeFunnet = false, harAnsvar = false),
    BEKREFTELSE_IKKE_FUNNET(periodeFunnet = true, harAnsvar = true),
    UVENTET_TILSTAND(periodeFunnet = true, harAnsvar = true)
}

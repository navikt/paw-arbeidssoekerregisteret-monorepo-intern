package no.nav.paw.bekreftelsetjeneste.tilstand

import arrow.core.NonEmptyList
import java.time.Duration
import java.time.Instant

fun Bekreftelse.erKlarForUtfylling(now: Instant, tilgjengeligOffset: Duration): Boolean =
    now.isAfter(gjelderTil.minus(tilgjengeligOffset))

fun Bekreftelse.harFristUtloept(now: Instant, tilgjengeligOffset: Duration): Boolean =
    now.isAfter(tilgjengeliggjort?.plus(tilgjengeligOffset) ?: gjelderTil)

fun Bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(now: Instant, varselFoerGraceperiodeUtloept: Duration): Boolean =
    sisteVarselOmGjenstaaendeGraceTid == null && now.isAfter(fristUtloept?.plus(varselFoerGraceperiodeUtloept) ?: gjelderTil.plus(
        varselFoerGraceperiodeUtloept
    ))

fun Bekreftelse.harGraceperiodeUtloept(now: Instant, graceperiode: Duration): Boolean =
    now.isAfter(fristUtloept?.plus(graceperiode) ?: gjelderTil.plus(graceperiode))

fun NonEmptyList<Bekreftelse>.shouldCreateNewBekreftelse(now: Instant, interval: Duration, tilgjengeligOffset: Duration): Boolean =
    maxBy { it.gjelderTil }
        .let {
            now.isAfter(it.gjelderTil.plus(interval.minus(tilgjengeligOffset)))
        }





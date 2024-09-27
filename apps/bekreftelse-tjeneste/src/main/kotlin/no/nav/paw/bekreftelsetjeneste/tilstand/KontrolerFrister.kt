package no.nav.paw.bekreftelsetjeneste.tilstand

import arrow.core.NonEmptyList
import java.time.Instant

fun Bekreftelse.erKlarForUtfylling(now: Instant): Boolean =
    now.isAfter(gjelderTil.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset))

fun Bekreftelse.harFristUtloept(now: Instant): Boolean =
    now.isAfter(tilgjengeliggjort?.plus(BekreftelseConfig.bekreftelseTilgjengeligOffset) ?: gjelderTil)

fun Bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(now: Instant): Boolean =
    sisteVarselOmGjenstaaendeGraceTid == null && now.isAfter(fristUtloept?.plus(BekreftelseConfig.varselFoerGracePeriodeUtloept) ?: gjelderTil.plus(
        BekreftelseConfig.varselFoerGracePeriodeUtloept
    ))

fun Bekreftelse.harGracePeriodeUtloept(now: Instant): Boolean =
    now.isAfter(fristUtloept?.plus(BekreftelseConfig.gracePeriode) ?: gjelderTil.plus(BekreftelseConfig.gracePeriode))

fun NonEmptyList<Bekreftelse>.shouldCreateNewBekreftelse(now: Instant): Boolean =
    maxBy { it.gjelderTil }
        .let {
            now.isAfter(it.gjelderTil.plus(BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset)))
        }





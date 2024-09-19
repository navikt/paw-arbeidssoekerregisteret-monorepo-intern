package no.nav.paw.bekreftelsetjeneste

import arrow.core.NonEmptyList
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import java.time.Instant

fun Bekreftelse.erKlarForUtfylling(now: Instant): Boolean =
    now.isAfter(gjelderTil.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset))

fun Bekreftelse.harFristUtloept(now: Instant): Boolean =
    now.isAfter(gjelderTil)

fun Bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(now: Instant): Boolean =
    sisteVarselOmGjenstaaendeGraceTid == null && now.isAfter(gjelderTil.plus(
        BekreftelseConfig.varselFoerGracePeriodeUtloept
    ))

fun Bekreftelse.harGracePeriodeUtloept(now: Instant): Boolean =
    now.isAfter(gjelderTil.plus(BekreftelseConfig.gracePeriode))

fun skalLageNyBekreftelseTilgjengelig(now: Instant, bekreftelser: NonEmptyList<Bekreftelse>): Boolean =
    now.isAfter(
        bekreftelser.maxOf { it.gjelderTil }.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset)
    )



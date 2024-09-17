package no.nav.paw.bekreftelsetjeneste

import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import java.time.Instant

fun Bekreftelse.erKlarForUtfylling(now: Instant): Boolean =
    gjelderTil.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).isAfter(now)

fun Bekreftelse.harFristUtloept(now: Instant): Boolean =
    gjelderTil.isBefore(now)

fun Bekreftelse.skalPurres(now: Instant): Boolean =
    sistePurring == null && gjelderTil.plus(
        BekreftelseConfig.varselFoerGracePeriodeUtloept
    ).isAfter(now)

fun Bekreftelse.harGracePeriodeUtloept(now: Instant): Boolean =
    gjelderTil.plus(BekreftelseConfig.gracePeriode)
        .isAfter(now)

fun Bekreftelse.skalLageNyBekreftelseTilgjengelig(now: Instant, bekreftelser: List<Bekreftelse>): Boolean =
    gjelderTil.plus(BekreftelseConfig.bekreftelseInterval)
        .minus(BekreftelseConfig.bekreftelseTilgjengeligOffset).isAfter(now) && bekreftelser.size < 2



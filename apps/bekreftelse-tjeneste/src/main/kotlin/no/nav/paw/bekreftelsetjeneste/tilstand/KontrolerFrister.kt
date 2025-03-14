package no.nav.paw.bekreftelsetjeneste.tilstand

import java.time.Duration
import java.time.Instant

fun Bekreftelse.erKlarForUtfylling(now: Instant, tilgjengeligOffset: Duration): Boolean =
    when (sisteTilstand()) {
        is IkkeKlarForUtfylling -> now.isAfter(publiseringstidForBekreftelse(gjelderTil, tilgjengeligOffset))
        else -> false
    }


fun Bekreftelse.harFristUtloept(now: Instant): Boolean =
    when (val gjeldeneTilstand = sisteTilstand()) {
        is KlarForUtfylling -> now.isAfter(gjelderTil)
        else -> false
    }


fun Bekreftelse.erSisteVarselOmGjenstaaendeGraceTid(now: Instant, varselFoerGraceperiodeUtloept: Duration): Boolean =
    when (val gjeldeneTilstand = sisteTilstand()) {
        is VenterSvar -> !has<GracePeriodeVarselet>() && now.isAfter(
            gjeldeneTilstand.timestamp.plus(
                varselFoerGraceperiodeUtloept
            )
        )

        else -> false
    }

fun Bekreftelse.harGraceperiodeUtloept(now: Instant, graceperiode: Duration): Boolean =
    when (val gjeldeneTilstand = sisteTilstand()) {
        is VenterSvar -> now.isAfter(gjeldeneTilstand.timestamp.plus(graceperiode))
        is GracePeriodeVarselet -> tilstandsLogg.get<VenterSvar>()?.timestamp?.let { ts ->  now.isAfter(ts.plus(graceperiode)) } ?: false
        else -> false
    }

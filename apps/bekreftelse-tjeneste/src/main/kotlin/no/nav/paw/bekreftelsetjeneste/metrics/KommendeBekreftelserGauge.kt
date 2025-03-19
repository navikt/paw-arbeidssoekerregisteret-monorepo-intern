package no.nav.paw.bekreftelsetjeneste.metrics

import io.micrometer.core.instrument.Tag
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.paavegneav.InternPaaVegneAv
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.IkkeKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.erKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import no.nav.paw.bekreftelsetjeneste.topology.norskTid
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.WeekFields


fun kommendeBekrefteler(
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon,
    now: Instant,
    tilstand: BekreftelseTilstand,
    ansvarlige: List<InternPaaVegneAv>
): WithMetricsInfo? {
    if (ansvarlige.isNotEmpty()) return null
    val neste = tilstand.bekreftelser
        .filter { it.sisteTilstand() is IkkeKlarForUtfylling }
        .minByOrNull { it.gjelderTil }
        ?: return null
    val terskel = bekreftelseKonfigurasjon.tilgjengeligOffset.multipliedBy(2)
    if (neste.erKlarForUtfylling(now, terskel)) {
        val dag = (Instant.now() - bekreftelseKonfigurasjon.tilgjengeligOffset)
            .let { LocalDateTime.ofInstant(it, norskTid) }
            .get(WeekFields.ISO.dayOfWeek())
            .let {
                when (it) {
                    1 -> "mandag"
                    2 -> "tirsdag"
                    3 -> "onsdag"
                    4 -> "torsdag"
                    5 -> "fredag"
                    6 -> "lørdag"
                    7 -> "søndag"
                    else -> "ukjent"
                }
            }
        return WithMetricsInfo(
            partition = tilstand.kafkaPartition,
            name = "kommende_bekreftelser",
            labels = listOf(
                Tag.of("dag", dag),
                Tag.of("innen", terskel.toHours().toString())
            )
        )
    } else {
        return null
    }
}
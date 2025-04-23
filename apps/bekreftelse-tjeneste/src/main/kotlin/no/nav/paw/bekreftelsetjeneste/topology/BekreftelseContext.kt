package no.nav.paw.bekreftelsetjeneste.topology

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo
import no.nav.paw.model.Identitetsnummer
import java.time.Instant
import java.time.ZoneId

private val tidssone = ZoneId.of("Europe/Oslo")

class BekreftelseContext(
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val konfigurasjon: BekreftelseKonfigurasjon,
    val wallClock: WallClock,
    val periodeInfo: PeriodeInfo
) {
    val identitetsnummer = Identitetsnummer(periodeInfo.identitetsnummer)

    fun tidligsteBekreftelsePeriodeStart(): Instant {
        return if (!periodeInfo.startet.isBefore(
                konfigurasjon.tidligsteBekreftelsePeriodeStart.atStartOfDay(tidssone).toInstant()
            )
        ) {
            periodeInfo.startet
        } else {
            konfigurasjon.tidligsteBekreftelsePeriodeStart.atStartOfDay(tidssone).toInstant()
        }
    }
}
package no.nav.paw.bekreftelsetjeneste.topology

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.config.tidligsteStartUke
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.*
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo
import no.nav.paw.model.Identitetsnummer
import java.time.Instant
import java.time.ZoneId

private val tidssone = ZoneId.of("Europe/Oslo")

class BekreftelseContext(
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val konfigurasjon: BekreftelseKonfigurasjon,
    val wallClock: WallClock,
    val periodeInfo: PeriodeInfo,
    private val oddetallPartallMap: OddetallPartallMap
) {
    val identitetsnummer = Identitetsnummer(periodeInfo.identitetsnummer)

    operator fun get(identitetsnummer: Identitetsnummer): Ukenummer = oddetallPartallMap[identitetsnummer]

    fun tidligsteBekreftelsePeriodeStart(): Instant {
        if (!periodeInfo.startet.isBefore(
                konfigurasjon.tidligsteBekreftelsePeriodeStart.atStartOfDay(tidssone).toInstant()
            )
        ) {
            return periodeInfo.startet
        }
        val partallOddetall = this[identitetsnummer]
        val tidligsteStartUke = when {
            konfigurasjon.tidligsteStartUke % 2 == 0 -> Partallsuke
            else -> Oddetallsuke
        }
        return if (partallOddetall == tidligsteStartUke || partallOddetall is Ukjent) {
            konfigurasjon.tidligsteBekreftelsePeriodeStart.atStartOfDay(tidssone).toInstant()
        } else {
            konfigurasjon.tidligsteBekreftelsePeriodeStart.plusWeeks(1).atStartOfDay(tidssone).toInstant()
        }
    }
}
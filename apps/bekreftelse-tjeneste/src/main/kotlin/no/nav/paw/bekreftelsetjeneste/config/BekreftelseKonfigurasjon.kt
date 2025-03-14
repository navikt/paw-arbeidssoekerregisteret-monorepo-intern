package no.nav.paw.bekreftelsetjeneste.config

import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.time.temporal.WeekFields

const val BEKREFTELSE_CONFIG_FILE_NAME = "bekreftelse_config.toml"

data class BekreftelseKonfigurasjon(
    val maksAntallVentendeBekreftelser: Int = 3,
    val tidligsteBekreftelsePeriodeStart: LocalDate,
    val interval: Duration,
    val graceperiode: Duration,
    val tilgjengeligOffset: Duration,
    val varselFoerGraceperiodeUtloept: Duration = graceperiode.dividedBy(2)
)

val BekreftelseKonfigurasjon.tidligsteStartUke: Int get() =
    tidligsteBekreftelsePeriodeStart.get(WeekFields.ISO.weekOfYear())
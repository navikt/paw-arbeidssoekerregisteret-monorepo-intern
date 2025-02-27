package no.nav.paw.bekreftelsetjeneste.config

import java.time.Duration
import java.time.Instant

const val BEKREFTELSE_CONFIG_FILE_NAME = "bekreftelse_config.toml"

data class BekreftelseKonfigurasjon(
    val migreringstidspunkt: Instant,
    val interval: Duration,
    val graceperiode: Duration,
    val tilgjengeligOffset: Duration,
    val varselFoerGraceperiodeUtloept: Duration = graceperiode.dividedBy(2)
)
package no.nav.paw.bekreftelse.config

import io.kotest.core.spec.style.FreeSpec
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

class VerifiseringLastingAvCfg: FreeSpec({
    "Verifisering av lasting av konfigurasjonsfiler" {
        val appCfg = loadNaisOrLocalConfiguration<ApplicationConfig>("application_config.toml")
        println(appCfg)
    }
})
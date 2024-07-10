package no.nav.paw.arbeidssoekerregisteret.config

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.properties.APPLICATION_CONFIG_FILE_NAME
import no.nav.paw.arbeidssoekerregisteret.properties.ApplicationProperties
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

class AppConfigTest : FreeSpec({
    "Skal laste config" {
        val appConfig = loadNaisOrLocalConfiguration<ApplicationProperties>(APPLICATION_CONFIG_FILE_NAME)
        appConfig.kafka shouldNotBe null
        appConfig.kafkaStreams shouldNotBe null
    }
})
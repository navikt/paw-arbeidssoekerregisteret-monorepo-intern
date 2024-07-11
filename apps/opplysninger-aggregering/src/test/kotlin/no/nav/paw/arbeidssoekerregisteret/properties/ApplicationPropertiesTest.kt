package no.nav.paw.arbeidssoekerregisteret.properties

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

class ApplicationPropertiesTest : FreeSpec({
    "Skal laste config" {
        val properties = loadNaisOrLocalConfiguration<ApplicationProperties>(APPLICATION_CONFIG_FILE_NAME)
        properties.kafka shouldNotBe null
        properties.kafkaStreams shouldNotBe null
    }
})
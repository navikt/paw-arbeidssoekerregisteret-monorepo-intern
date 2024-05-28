package no.nav.paw.rapportering.internehendelser

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.util.*

class SerdeTest : FreeSpec({
    "Enkel Serde test" - {
        "${RapporteringsHendelseSerde::class.simpleName} kan serialisere og deserialisere" {
            val hendelse = LeveringsfristUtloept(
                hendelseId = UUID.randomUUID(),
                periodeId = UUID.randomUUID(),
                identitetsnummer = "12345678901",
                rapporteringsId = UUID.randomUUID(),
                arbeidssoekerId = 1234567890L
            )
            val resultat = RapporteringsHendelseSerializer.serialize("", hendelse)
                .let { serialized ->
                    println("serialized: ${String(serialized!!)}")
                    RapporteringsHendelseDeserializer.deserialize("", serialized)
                }
            resultat shouldBe hendelse
        }
    }
})
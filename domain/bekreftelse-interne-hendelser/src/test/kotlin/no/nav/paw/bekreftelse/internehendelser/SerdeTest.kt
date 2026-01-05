package no.nav.paw.bekreftelse.internehendelser

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class SerdeTest : FreeSpec({
    "Enkel Serde test" - {
        "${BekreftelseHendelseSerde::class.simpleName} kan serialisere og deserialisere" {
            val hendelse = LeveringsfristUtloept(
                hendelseId = UUID.randomUUID(),
                periodeId = UUID.randomUUID(),
                bekreftelseId = UUID.randomUUID(),
                arbeidssoekerId = 1234567890L,
                hendelseTidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                leveringsfrist = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            )
            val resultat = BekreftelseHendelseSerializer().serialize("", hendelse)
                .let { serialized ->
                    println("serialized: ${String(serialized!!)}")
                    BekreftelseHendelseDeserializer().deserialize("", serialized)
                }
            resultat shouldBe hendelse
        }
    }
})
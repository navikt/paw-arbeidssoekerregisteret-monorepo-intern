package no.nav.paw.bekreftelseutgang.tilstand

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at tilstand i state store skrevet med Jackson 2 kan leses av gjeldende
 * InternTilstandSerde. JSON-filene er generert med Jackson 2, se [lagretJson].
 */
class InternTilstandSerdeKompatibilitetTest : FreeSpec({
    val serde = InternTilstandSerde()

    eksempler.forEach { (navn, forventet) ->
        "$navn skrevet med Jackson 2 kan leses" {
            val json = lagretJson("bekreftelse-utgang/$navn") {
                serde.serializer().serialize("topic", forventet)
            }
            serde.deserializer().deserialize("topic", json) shouldBe forventet
        }
    }
})

private val PERIODE_ID = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d")

private val eksempler: List<Pair<String, InternTilstand>> = listOf(
    "komplett" to InternTilstand(
        identitetsnummer = "12345678901",
        bekreftelseHendelse = RegisterGracePeriodeUtloept(
            hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000201"),
            periodeId = PERIODE_ID,
            arbeidssoekerId = 1001L,
            hendelseTidspunkt = Instant.parse("2025-03-14T09:26:53.589Z"),
            bekreftelseId = UUID.fromString("5f6a7b8c-9d0e-4f1a-8b2c-3d4e5f6a7b8c"),
            kilde = "test-kilde"
        )
    ),
    "kun-hendelse" to InternTilstand(
        identitetsnummer = null,
        bekreftelseHendelse = PeriodeAvsluttet(
            hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000202"),
            periodeId = PERIODE_ID,
            arbeidssoekerId = 1001L,
            hendelseTidspunkt = Instant.parse("2025-03-14T09:26:53Z")
        )
    ),
    "kun-identitetsnummer" to InternTilstand(
        identitetsnummer = "12345678901",
        bekreftelseHendelse = null
    )
)

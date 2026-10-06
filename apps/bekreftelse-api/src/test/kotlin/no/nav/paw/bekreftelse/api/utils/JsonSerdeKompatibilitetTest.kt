package no.nav.paw.bekreftelse.api.utils

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at BekreftelseTilgjengelig lagret i JSONB-kolonnen bekreftelser.data
 * med Jackson 2 kan leses av gjeldende JsonSerde. JSON-filene er generert med
 * Jackson 2, se [lagretJson].
 */
class JsonSerdeKompatibilitetTest : FreeSpec({
    eksempler.forEach { (navn, forventet) ->
        "BekreftelseTilgjengelig $navn lagret med Jackson 2 kan leses" {
            val json = lagretJson("bekreftelse-api/$navn") { JsonSerde.serialize(forventet) }
            JsonSerde.deserialize(json) shouldBe forventet
        }
    }
})

private val eksempler: List<Pair<String, BekreftelseTilgjengelig>> = listOf(
    "bekreftelse-tilgjengelig" to BekreftelseTilgjengelig(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000301"),
        periodeId = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"),
        arbeidssoekerId = 1001L,
        hendelseTidspunkt = Instant.parse("2025-03-14T09:26:53.589Z"),
        bekreftelseId = UUID.fromString("5f6a7b8c-9d0e-4f1a-8b2c-3d4e5f6a7b8c"),
        gjelderFra = Instant.parse("2025-03-02T23:00:00Z"),
        gjelderTil = Instant.parse("2025-03-16T23:00:00Z")
    ),
    "bekreftelse-tilgjengelig-nanosekunder" to BekreftelseTilgjengelig(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000302"),
        periodeId = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d"),
        arbeidssoekerId = 1001L,
        hendelseTidspunkt = Instant.parse("2025-03-14T09:26:53.589793238Z"),
        bekreftelseId = UUID.fromString("6a7b8c9d-0e1f-4a2b-8c3d-4e5f6a7b8c9d"),
        gjelderFra = Instant.parse("2025-03-16T23:00:00Z"),
        gjelderTil = Instant.parse("2025-03-30T22:00:00.000000001Z")
    )
)

package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.bqadapter.Encoder
import java.time.Instant
import java.util.UUID

class EgenvurderingSchemaTest : FreeSpec({
    val encoder = Encoder(
        identSalt = "ident-salt".toByteArray(),
        periodeIdSalt = "periode-salt".toByteArray()
    )

    "egenvurderingSchema" - {
        "should define required fields" {
            egenvurderingSchema.fields.map { it.name to it.mode } shouldContainExactly listOf(
                "correlation_id" to Field.Mode.REQUIRED,
                "sendt_inn_av" to Field.Mode.REQUIRED,
                "profilert_til" to Field.Mode.REQUIRED,
                "egenvurdering" to Field.Mode.REQUIRED
            )
        }
    }

    "egenvurderingRad" - {
        "should map fields without exposing raw ids" {
            val egenvurdering = egenvurdering()

            val row = egenvurderingRad(encoder, egenvurdering)

            row["correlation_id"] shouldBe encoder.encodePeriodeId(egenvurdering.periodeId)
            row shouldNotContainKey "id"
            row shouldNotContainKey "profilering_id"
            @Suppress("UNCHECKED_CAST")
            val metadata = row["sendt_inn_av"] as Map<String, Any>
            metadata shouldNotContainKey "id"
            metadata shouldContainExactly mapOf(
                "tidspunkt" to "2026-09-03",
                "kilde" to "egenvurdering",
                "aarsak" to "sendt_inn",
                "brukertype" to "sluttbruker"
            )
            row["profilert_til"] shouldBe "antatt_behov_for_veiledning"
            row["egenvurdering"] shouldBe "oppgitt_hindringer"
        }
    }
})

private fun egenvurdering() = Egenvurdering(
    UUID.fromString("1b275acf-f481-4c63-b56a-4e0c927e6ce7"),
    UUID.fromString("9d3fa766-cca6-4825-9841-bfb63d0ccac2"),
    UUID.fromString("39542bbb-d6d1-472d-9776-78f0ebdf64d1"),
    Metadata(
        Instant.parse("2026-09-03T06:00:00Z"),
        Bruker(BrukerType.SLUTTBRUKER, "test-bruker", "tokenx:Level4"),
        "egenvurdering",
        "sendt_inn",
        null
    ),
    ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
    ProfilertTil.OPPGITT_HINDRINGER
)

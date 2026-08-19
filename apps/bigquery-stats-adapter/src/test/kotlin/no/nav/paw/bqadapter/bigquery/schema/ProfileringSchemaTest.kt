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
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.bqadapter.Encoder
import java.time.Instant
import java.util.UUID

class ProfileringSchemaTest : FreeSpec({
    val encoder = Encoder(
        identSalt = "ident-salt".toByteArray(),
        periodeIdSalt = "periode-salt".toByteArray()
    )

    "profileringSchema" - {
        "should define required fields" {
            profileringSchema.fields.map { it.name to it.mode } shouldContainExactly listOf(
                "correlation_id" to Field.Mode.REQUIRED,
                "opplysninger_id" to Field.Mode.REQUIRED,
                "sendt_inn_av" to Field.Mode.REQUIRED,
                "profilert_til" to Field.Mode.REQUIRED,
                "jobbet_sammenhengende_seks_av_tolv_siste_mnd" to Field.Mode.REQUIRED,
                "aldersgruppe" to Field.Mode.REQUIRED
            )
        }
    }

    "profileringRad" - {
        "should map profiling fields without exposing raw ids" {
            val profilering = profilering(alder = 42)

            val row = profileringRad(encoder, profilering)

            row["correlation_id"] shouldBe encoder.encodePeriodeId(profilering.periodeId)
            row["opplysninger_id"] shouldBe
                    encoder.encodeOpplysningsId(profilering.opplysningerOmArbeidssokerId)
            row shouldNotContainKey "profilering_id"
            @Suppress("UNCHECKED_CAST")
            val metadata = row["sendt_inn_av"] as Map<String, Any>
            metadata shouldNotContainKey "id"
            metadata shouldContainExactly mapOf(
                "tidspunkt" to "2026-08-19",
                "kilde" to "profilering",
                "aarsak" to "opplysninger_mottatt",
                "brukertype" to "system"
            )
            row["profilert_til"] shouldBe "antatt_gode_muligheter"
            row["jobbet_sammenhengende_seks_av_tolv_siste_mnd"] shouldBe true
            row shouldNotContainKey "alder"
            row["aldersgruppe"] shouldBe "40_49"
        }

        "should group age at defined boundaries" {
            mapOf(
                null to "ukjent",
                -1 to "ugyldig",
                0 to "under_30",
                29 to "under_30",
                30 to "30_39",
                39 to "30_39",
                40 to "40_49",
                49 to "40_49",
                50 to "50_59",
                59 to "50_59",
                60 to "60_pluss"
            ).forEach { (alder, forventetGruppe) ->
                profileringRad(encoder, profilering(alder))["aldersgruppe"] shouldBe forventetGruppe
            }
        }

    }
})

private fun profilering(alder: Int?) = Profilering(
    UUID.fromString("9d3fa766-cca6-4825-9841-bfb63d0ccac2"),
    UUID.fromString("39542bbb-d6d1-472d-9776-78f0ebdf64d1"),
    UUID.fromString("c52ce702-c12f-49ab-a064-bb504613d680"),
    Metadata(
        Instant.parse("2026-08-19T06:00:00Z"),
        Bruker(BrukerType.SYSTEM, "profilering:1", null),
        "profilering",
        "opplysninger_mottatt",
        null
    ),
    ProfilertTil.ANTATT_GODE_MULIGHETER,
    true,
    alder
)

package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import no.nav.paw.bqadapter.Encoder
import java.time.Instant
import java.util.UUID

class OpplysningerSchemaTest : FreeSpec({
    val encoder = Encoder(
        identSalt = "ident-salt".toByteArray(),
        periodeIdSalt = "periode-salt".toByteArray()
    )

    "opplysningerSchema" - {
        "should define required, nullable and repeated fields" {
            opplysningerSchema.fields.map { it.name to it.mode } shouldContainExactly listOf(
                "correlation_id" to Field.Mode.REQUIRED,
                "opplysninger_id" to Field.Mode.REQUIRED,
                "sendt_inn_av" to Field.Mode.REQUIRED,
                "utdanning" to Field.Mode.NULLABLE,
                "helse" to Field.Mode.NULLABLE,
                "jobbsituasjoner" to Field.Mode.REPEATED,
                "annet" to Field.Mode.NULLABLE
            )
            opplysningerSchema.fields
                .first { it.name == "jobbsituasjoner" }
                .subFields
                .map { it.name to it.mode } shouldContainExactly listOf(
                "beskrivelse" to Field.Mode.REQUIRED,
                "stilling_styrk08" to Field.Mode.NULLABLE
            )
        }
    }

    "opplysningerRad" - {
        "should map all fields without exposing the submitter id" {
            val opplysninger = opplysninger(
                utdanning = Utdanning("7", JaNeiVetIkke.JA, JaNeiVetIkke.NEI),
                helse = Helse(JaNeiVetIkke.NEI),
                jobbsituasjon = Jobbsituasjon(
                    listOf(
                        BeskrivelseMedDetaljer(
                            Beskrivelse.ER_PERMITTERT,
                            linkedMapOf(
                                "stilling_styrk08" to "7213",
                                "stilling" to "Bilskadereparatør",
                                "prosent" to "50",
                                "gjelder_fra_dato_iso8601" to "2026-08-01"
                            )
                        ),
                        BeskrivelseMedDetaljer(
                            Beskrivelse.ANNET,
                            mapOf("stilling" to "Skal ikke eksporteres")
                        )
                    )
                ),
                annet = Annet(JaNeiVetIkke.VET_IKKE)
            )

            val row = opplysningerRad(encoder, opplysninger)

            row["correlation_id"] shouldBe encoder.encodePeriodeId(opplysninger.periodeId)
            row["opplysninger_id"] shouldBe encoder.encodeOpplysningsId(opplysninger.id)
            @Suppress("UNCHECKED_CAST")
            val metadata = row["sendt_inn_av"] as Map<String, Any>
            metadata shouldNotContainKey "id"
            metadata shouldContainExactly mapOf(
                "tidspunkt" to "2026-08-18",
                "kilde" to "test",
                "aarsak" to "registrering",
                "brukertype" to "sluttbruker"
            )
            row["utdanning"] shouldBe mapOf(
                "nus" to "7",
                "bestaatt" to "ja",
                "godkjent" to "nei"
            )
            row["helse"] shouldBe mapOf("helsetilstand_hindrer_arbeid" to "nei")
            row["jobbsituasjoner"] shouldBe listOf(
                mapOf(
                    "beskrivelse" to "er_permittert",
                    "stilling_styrk08" to "7213"
                ),
                mapOf("beskrivelse" to "annet")
            )
            row["annet"] shouldBe mapOf("andre_forhold_hindrer_arbeid" to "vet_ikke")
        }

        "should omit optional fields when they are absent" {
            val row = opplysningerRad(
                encoder = encoder,
                opplysninger = opplysninger(
                    utdanning = null,
                    helse = null,
                    jobbsituasjon = Jobbsituasjon(emptyList()),
                    annet = null
                )
            )

            row shouldNotContainKey "utdanning"
            row shouldNotContainKey "helse"
            row shouldNotContainKey "annet"
            row["jobbsituasjoner"] shouldBe emptyList<Map<String, Any>>()
        }
    }
})

private fun opplysninger(
    utdanning: Utdanning?,
    helse: Helse?,
    jobbsituasjon: Jobbsituasjon,
    annet: Annet?
) = OpplysningerOmArbeidssoeker(
    UUID.fromString("c52ce702-c12f-49ab-a064-bb504613d680"),
    UUID.fromString("39542bbb-d6d1-472d-9776-78f0ebdf64d1"),
    Metadata(
        Instant.parse("2026-08-18T10:00:00Z"),
        Bruker(BrukerType.SLUTTBRUKER, "12345678910", "tokenx:Level4"),
        "test",
        "registrering",
        null
    ),
    utdanning,
    helse,
    jobbsituasjon,
    annet
)

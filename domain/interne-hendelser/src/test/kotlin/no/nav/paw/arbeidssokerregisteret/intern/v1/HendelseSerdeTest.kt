package no.nav.paw.arbeidssokerregisteret.intern.v1

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning

class HendelseSerdeTest: FreeSpec({
    val serde = HendelseSerde()
    "Test default verdier" - {
        "Verifiser at vi kan deserialisere hendelse uten opplysninger" {
            serde.deserializer().deserializeFromString(avvistUtenOpplysninger)
        }
        "Verifisert at vi kan deserialisere hendelse med ukjent opplysning" {
            val resultat = serde.deserializer().deserializeFromString(avvistMedUkjentOpplysning)
            resultat.shouldBeInstanceOf<Avvist>()
            resultat.opplysninger shouldContain Opplysning.IKKE_ANSATT
            resultat.opplysninger shouldContain Opplysning.UKJENT_OPPLYSNING
            resultat.opplysninger.size shouldBe 2
        }
    }
})


val avvistUtenOpplysninger: String = """
{
    "hendelseId":"723d5d09-83c7-4f83-97fd-35f7c9c5c798",
    "id":1,
    "identitetsnummer":"12345678901",
    "metadata":{
        "tidspunkt":1630404930.000000000,
        "utfoertAv":{
            "type":"SYSTEM",
            "id":"Testsystem"
        },
        "kilde":"Testkilde",
        "aarsak":"Testaarsak"
    },
    "hendelseType":"intern.v1.avvist"
} 
""".trimIndent()

val avvistMedUkjentOpplysning: String = """
{
    "hendelseId":"723d5d09-83c7-4f83-97fd-35f7c9c5c798",
    "id":1,
    "identitetsnummer":"12345678901",
    "metadata":{
        "tidspunkt":1630404930.000000000,
        "utfoertAv":{
            "type":"SYSTEM",
            "id":"Testsystem"
        },
        "kilde":"Testkilde",
        "aarsak":"Testaarsak"
    },
    "hendelseType":"intern.v1.avvist",
    "opplysninger":["${Opplysning.IKKE_ANSATT}", "udefinert opplysning"]
} 
""".trimIndent()
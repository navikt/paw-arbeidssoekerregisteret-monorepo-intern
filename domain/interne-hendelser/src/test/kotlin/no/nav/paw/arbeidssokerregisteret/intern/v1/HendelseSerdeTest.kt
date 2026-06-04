package no.nav.paw.arbeidssokerregisteret.intern.v1

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvsluttetAarsakType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.RegelEvalResultat

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
        "Verifiser at deserialisering av gamle hendelser fremdeles fungerer" - {
            "Vi kan deserialisere en 'avsluttet' hendelse generert i dev 2. juni 2026" {
                val resultat = serde.deserializer().deserializeFromString(avsluttet_hendelse_dev_02062026)
                resultat.shouldBeInstanceOf<Avsluttet>()
                resultat.aarsaksInformasjon shouldBe null
            }
        }
        "Verifiser deserialisering av avsluttetAarsak" - {
            "Kjent type deserialiseres korrekt" {
                val resultat = serde.deserializer().deserializeFromString(avsluttet_hendelse_med_ny_aarsak)
                resultat.shouldBeInstanceOf<Avsluttet>()
                resultat.aarsaksInformasjon?.aarsak shouldBe AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE
                resultat.aarsaksInformasjon?.regelEvalResultat shouldBe RegelEvalResultat.IKKE_RELEVANT
            }
            "Ukjent type gir UKJENT_VERDI" {
                val resultat = serde.deserializer().deserializeFromString(avsluttet_med_ukjent_aarsak_type)
                resultat.shouldBeInstanceOf<Avsluttet>()
                resultat.aarsaksInformasjon?.aarsak shouldBe AvsluttetAarsakType.UKJENT_VERDI
                resultat.aarsaksInformasjon?.regelEvalResultat shouldBe RegelEvalResultat.UKJENT_VERDI
            }
        }
    }
})

val avsluttet_hendelse_dev_02062026 = """
{
    "hendelseId" : "af52f6f9-ebd5-4cd3-eeb4-3aefdecd7b12",
    "id" : 139682,
    "identitetsnummer" : "45419942222",
    "metadata" : {
        "tidspunkt":1780358119.801662,
        "utfoertAv":{
        "type":"SYSTEM",
        "id":"europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssoekerregisteret-bekreftelse-utgang:26.05.29.327-1",
        "sikkerhetsnivaa": null
    },
    "kilde" : "bekreftelse_paavegneav_stopp:dagpenger",
    "aarsak" : "[Bekreftelse:ytelse/støtte] Ikke levert innen fristen",
    "tidspunktFraKilde" : null
    },
    "opplysninger":[
    ],
    "periodeId" : "12b794c9-2d77-484f-8117-589a0d413566",
    "kalkulertAarsak" : "Udefinert",
    "oppgittAarsak" : "RegisterGracePeriodeUtloeptEtterEksternInnsamling",
    "hendelseType" : "intern.v1.avsluttet"
}
""".trimIndent()

val avsluttet_hendelse_med_ny_aarsak = """
{
    "hendelseId" : "af52f6f9-ebd5-4cd3-eeb4-3aefdecd7b12",
    "id" : 139682,
    "identitetsnummer" : "45419942222",
    "metadata" : {
        "tidspunkt":1780358119.801662,
        "utfoertAv":{
        "type":"SYSTEM",
        "id":"europe-north1-docker.pkg.dev/nais-management-233d/paw/paw-arbeidssoekerregisteret-bekreftelse-utgang:26.05.29.327-1",
        "sikkerhetsnivaa": null
    },
    "kilde" : "bekreftelse_paavegneav_stopp:dagpenger",
    "aarsak" : "[Bekreftelse:ytelse/støtte] Ikke levert innen fristen",
    "tidspunktFraKilde" : null
    },
    "opplysninger":[
    ],
    "periodeId" : "12b794c9-2d77-484f-8117-589a0d413566",
    "kalkulertAarsak" : "Udefinert",
    "aarsaksInformasjon" : {
        "aarsak": "SVARTE_NEI_I_BEKREFTELSE",
        "regelEvalResultat" : "IKKE_RELEVANT"
    },
    "hendelseType" : "intern.v1.avsluttet"
}
""".trimIndent()

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

val avsluttet_med_aarsak: String = """
{
    "hendelseId": "bf52f6f9-ebd5-4cd3-eeb4-3aefdecd7b12",
    "id": 139683,
    "identitetsnummer": "12345678901",
    "metadata": {
        "tidspunkt": 1780358119.801662,
        "utfoertAv": {
            "type": "SLUTTBRUKER",
            "id": "12345678901",
            "sikkerhetsnivaa": null
        },
        "kilde": "bekreftelse-utgang",
        "aarsak": "[Bekreftelse] Ønsket ikke lenger å være arbeidssøker",
        "tidspunktFraKilde": null
    },
    "opplysninger": [],
    "periodeId": "22b794c9-2d77-484f-8117-589a0d413566",
    "avsluttetAarsak": { "type": "SVARTE_NEI_I_BEKREFTELSE" },
    "hendelseType": "intern.v1.avsluttet"
}
""".trimIndent()

val avsluttet_med_ukjent_aarsak_type: String = """{
    "hendelseId": "cf52f6f9-ebd5-4cd3-eeb4-3aefdecd7b12",
    "id": 139684,
    "identitetsnummer": "12345678901",
    "metadata": {
        "tidspunkt": 1780358119.801662,
        "utfoertAv": {
            "type": "SYSTEM",
            "id": "test-app",
            "sikkerhetsnivaa": null
        },
        "kilde": "test",
        "aarsak": "test",
        "tidspunktFraKilde": null
    },
    "opplysninger": [],
    "periodeId": "32b794c9-2d77-484f-8117-589a0d413566",
    "aarsaksInformasjon": { "aarsak": "FREMTIDIG_UKJENT_AARSAK", "regelEvalResultat": "FREMTIDIG_UKJENT_EVAL_RESULTAT" },
    "hendelseType": "intern.v1.avsluttet"
}
""".trimIndent()
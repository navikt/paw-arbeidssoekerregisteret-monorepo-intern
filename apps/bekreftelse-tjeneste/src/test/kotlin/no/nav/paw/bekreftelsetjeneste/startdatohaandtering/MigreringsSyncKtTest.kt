package no.nav.paw.bekreftelsetjeneste.startdatohaandtering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.model.asIdentitetsnummer
import java.nio.file.Paths


class MigreringsSyncKtTest: FreeSpec({
    fun pathOf(filename: String) = Paths.get("src", "test", "resources", filename)
    val testfil1 = pathOf("ukenummer.csv")
    val testfil2 = pathOf("ukenummer2.csv")

    "Vi kan lese inn en gyldig csv fil uten header: $testfil1" {
        val data = oddetallPartallMapFraCsvFil(
            header = false,
            filer = listOf(testfil1),
            delimiter = ",",
            identitetsnummerKolonne = 0,
            ukenummerKolonne = 1,
            partall = "P",
            oddetall = "O"
        )
        val forventet = mapOf(
            "12345678910".asIdentitetsnummer() to Partallsuke,
            "12345678912".asIdentitetsnummer() to Partallsuke,
            "12345678909".asIdentitetsnummer() to Oddetallsuke,
            "12345678911".asIdentitetsnummer() to Oddetallsuke,
            "12345678913".asIdentitetsnummer() to Oddetallsuke,
            "12345678914".asIdentitetsnummer() to Partallsuke,
            "12345678916".asIdentitetsnummer() to Partallsuke,
            "12345678915".asIdentitetsnummer() to Oddetallsuke,
            "12345678918".asIdentitetsnummer() to Partallsuke,
            "12345678917".asIdentitetsnummer() to Oddetallsuke,
            "12345678919".asIdentitetsnummer() to Oddetallsuke
        )

        forventet.forEach { (identitetsnummer, forventetUke) ->
            data[identitetsnummer] shouldBe forventetUke
        }
    }

    "Vi kan lese inn en gyldig csv fil med header: $testfil1" {
        val data = oddetallPartallMapFraCsvFil(
            header = true,
            filer = listOf(testfil2),
            delimiter = ";",
            identitetsnummerKolonne = 1,
            ukenummerKolonne = 0,
            partall = "0",
            oddetall = "1"
        )
        val forventet = mapOf(
            "12345678910".asIdentitetsnummer() to Partallsuke,
            "12345678912".asIdentitetsnummer() to Partallsuke,
            "12345678909".asIdentitetsnummer() to Oddetallsuke,
            "12345678911".asIdentitetsnummer() to Oddetallsuke,
            "12345678913".asIdentitetsnummer() to Oddetallsuke,
            "12345678914".asIdentitetsnummer() to Partallsuke,
            "12345678916".asIdentitetsnummer() to Partallsuke,
            "12345678915".asIdentitetsnummer() to Oddetallsuke,
            "12345678918".asIdentitetsnummer() to Partallsuke,
            "12345678917".asIdentitetsnummer() to Oddetallsuke,
            "12345678919".asIdentitetsnummer() to Oddetallsuke
        )

        forventet.forEach { (identitetsnummer, forventetUke) ->
            data[identitetsnummer] shouldBe forventetUke
        }
    }
})
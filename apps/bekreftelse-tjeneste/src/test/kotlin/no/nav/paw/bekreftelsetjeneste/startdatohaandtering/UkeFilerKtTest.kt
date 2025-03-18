package no.nav.paw.bekreftelsetjeneste.startdatohaandtering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.paw.model.asIdentitetsnummer
import java.nio.file.Paths

class UkeFilerKtTest : FreeSpec({
    "Sjekk at vi finner alle filene" - {
        val basePath = Paths.get("src", "test", "resources", "test")
        val filer = finnFiler(basePath)
        "Alle v1.csv filene skal være med" {
            filer.shouldContainExactlyInAnyOrder(
                Paths.get("src", "test", "resources", "test", "paw-arbeidssoekere-bekreftelse-uke-sync-b", "v1.csv"),
                Paths.get("src", "test", "resources", "test", "paw-arbeidssoekere-bekreftelse-uke-sync-a", "v1.csv"),
            )
        }
        "Listen kan brukes til å lese inn filene" {
            val data = oddetallPartallMapFraCsvFil(
                header = false,
                filer = filer,
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
    }
})
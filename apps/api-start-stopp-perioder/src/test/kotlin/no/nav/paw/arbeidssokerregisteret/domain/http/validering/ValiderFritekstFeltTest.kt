package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import arrow.core.right
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.TestData

class ValiderFritekstFeltTest : FreeSpec({
    "Alle eksisterende 'stilling' verdier fra prod går igjennom valideringen" - {
        val testFile = "/stillinger_brukt_i_prod.txt"
        val testData = this::class.java.getResourceAsStream(testFile)
        testData.shouldNotBeNull()
        val stillinger = testData.use {
            it.reader().readLines()
        }.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        withData(stillinger) { stilling ->
            "$stilling skal være gyldig" {
                validerFritekstFelt("stilling", stilling) shouldBe Unit.right()
            }
        }
    }
    "Tekster med ugyldige tegn blir avvist" - {
        withData(
            "Vi < AS",
            "Vi er et > AS",
            "Vi er et <AS>",
            "Vi er et { AS",
            "Vi er et } AS",
            "Vi er et {AS}",
            "{Test",
            "Select; AS"
        ) { tekst ->
            "Teksten '$tekst' skal være ugyldig" {
                validerFritekstFelt("test", tekst).isLeft() shouldBe true
            }
        }
    }
})

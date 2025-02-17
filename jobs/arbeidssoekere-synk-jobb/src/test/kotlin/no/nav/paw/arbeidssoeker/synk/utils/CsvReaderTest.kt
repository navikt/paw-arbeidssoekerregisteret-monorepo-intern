package no.nav.paw.arbeidssoeker.synk.utils

import io.kotest.core.spec.style.FreeSpec

class CsvReaderTest : FreeSpec({
    "Skal lese CSV-fil" {
        val url = javaClass.getResource("/v1.csv")!!
        val values = ArbeidssoekerCsvReader.readValues(url.toURI())
        while (values.hasNextValue()) {
            val arbeidssoeker = values.nextValue()
            println(arbeidssoeker.identitetsnummer)
        }
    }
})
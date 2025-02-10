package no.nav.paw.arbeidssoeker.synk.utils

import io.kotest.core.spec.style.FreeSpec

class CsvReaderTest : FreeSpec({
    "Skal lese CSV-fil" {
        val url = javaClass.getResource("/sync.csv")!!
        val values = ArbeidssoekerCsvReader.readValues(url)
        while (values.hasNextValue()) {
            val arbeidssoeker = values.nextValue()
            println(arbeidssoeker.identitetsnummer)
        }
    }
})
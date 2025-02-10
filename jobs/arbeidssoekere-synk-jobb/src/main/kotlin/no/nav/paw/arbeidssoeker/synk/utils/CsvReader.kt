package no.nav.paw.arbeidssoeker.synk.utils

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.paw.arbeidssoeker.synk.model.Arbeidssoeker
import java.io.File
import java.net.URL

private val csvMapper: ObjectMapper = CsvMapper()
    .registerModule(KotlinModule.Builder().build())
private val csvSchema: CsvSchema = CsvSchema.builder()
    .setAllowComments(true)
    .setColumnSeparator(',')
    .setUseHeader(true)
    .build()

sealed class CsvReader<T>(val objectReader: ObjectReader) {
    fun readValues(file: File): MappingIterator<T> = objectReader.readValues(file)
    fun readValues(url: URL): MappingIterator<T> = objectReader.readValues(url)
}

data object ArbeidssoekerCsvReader : CsvReader<Arbeidssoeker>(
    objectReader = csvMapper
        .readerFor(Arbeidssoeker::class.java)
        .with(csvSchema)
)
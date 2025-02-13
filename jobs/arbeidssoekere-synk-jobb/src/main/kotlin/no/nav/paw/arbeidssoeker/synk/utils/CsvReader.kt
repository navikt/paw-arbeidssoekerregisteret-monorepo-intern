package no.nav.paw.arbeidssoeker.synk.utils

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.paw.arbeidssoeker.synk.model.Arbeidssoeker
import java.net.URI
import java.nio.file.Path

private val csvMapper: ObjectMapper = CsvMapper()
    .registerModule(KotlinModule.Builder().build())
private val csvSchema: CsvSchema = CsvSchema.builder()
    .setAllowComments(true)
    .setColumnSeparator(',')
    .setUseHeader(true)
    .build()

sealed class CsvReader<T>(val objectReader: ObjectReader) {
    fun readValues(uri: URI): MappingIterator<T> = objectReader.readValues(uri.toURL())
    fun readValues(path: Path): MappingIterator<T> {
        if (!java.nio.file.Files.exists(path)) {
            throw IllegalStateException("$path ikke funnet")
        }
        if (!java.nio.file.Files.isRegularFile(path)) {
            throw IllegalStateException("$path er ikke en fil")
        }
        if (!java.nio.file.Files.isReadable(path)) {
            throw IllegalStateException("$path kan ikke leses fra")
        }
        return readValues(path.toUri())
    }
}

data object ArbeidssoekerCsvReader : CsvReader<Arbeidssoeker>(
    objectReader = csvMapper
        .readerFor(Arbeidssoeker::class.java)
        .with(csvSchema)
)
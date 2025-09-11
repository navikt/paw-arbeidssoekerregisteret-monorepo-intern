package no.nav.paw.arbeidssoeker.synk.utils

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.model.ArbeidssoekerFileRow
import java.net.URI
import java.nio.file.Path
import kotlin.reflect.KClass

private fun String.asChar(): Char {
    if (length != 1) {
        throw IllegalArgumentException("$this is not a char")
    } else {
        return this[0]
    }
}

private fun buildCsvSchema(
    columnSeparator: Char,
    userHeader: Boolean,
    allowComments: Boolean
): CsvSchema = CsvSchema.builder()
    .setColumnSeparator(columnSeparator)
    .setUseHeader(userHeader)
    .setAllowComments(allowComments)
    .addColumn("identitetsnummer", CsvSchema.ColumnType.STRING)
    .addColumn("tidspunktFraKilde", CsvSchema.ColumnType.STRING)
    .build()

abstract class CsvReader<T : Any>(csvSchema: CsvSchema, kClass: KClass<T>) {
    private val csvReader: ObjectReader = CsvMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
        .readerFor(kClass.java)
        .with(csvSchema)

    fun readValues(uri: URI): MappingIterator<T> = csvReader.readValues(uri.toURL().openStream())
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

class ArbeidssoekerCsvReader(
    jobConfig: JobConfig
) : CsvReader<ArbeidssoekerFileRow>(
    buildCsvSchema(
        columnSeparator = jobConfig.csvFil.kolonneSeparator.asChar(),
        userHeader = jobConfig.csvFil.innholderHeader,
        allowComments = jobConfig.csvFil.inneholderKommentarer
    ), ArbeidssoekerFileRow::class
)
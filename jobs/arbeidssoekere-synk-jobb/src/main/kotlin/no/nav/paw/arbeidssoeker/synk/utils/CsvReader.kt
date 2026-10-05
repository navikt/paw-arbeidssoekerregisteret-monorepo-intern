package no.nav.paw.arbeidssoeker.synk.utils

import tools.jackson.databind.MappingIterator
import tools.jackson.databind.ObjectReader
import tools.jackson.dataformat.csv.CsvMapper
import tools.jackson.dataformat.csv.CsvSchema
import tools.jackson.module.kotlin.KotlinModule
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
    private val csvReader: ObjectReader = CsvMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()
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
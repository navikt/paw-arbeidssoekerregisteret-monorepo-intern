package no.nav.paw.bqadapter.bigquery.schema.structs

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.StandardSQLTypeName
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDate.ofInstant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val metadata_tidspunkt = "tidspunkt"
private const val metadata_kilde = "kilde"
private const val metadata_aarsak = "aarsak"
private const val metadata_brukertype = "brukertype"
private val localTimeZone = ZoneOffset.of("Europe/Oslo")
private val dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

val metadataStruct get(): FieldList = FieldList.of(
    Field.of(metadata_tidspunkt, StandardSQLTypeName.DATE),
    Field.of(metadata_kilde, StandardSQLTypeName.STRING),
    Field.of(metadata_aarsak, StandardSQLTypeName.STRING),
    Field.of(metadata_brukertype, StandardSQLTypeName.STRING)
)

fun metadataStruct(
    tidspunkt: Instant,
    kilde: String,
    aarsak: String,
    brukertype: String
): Map<String, Any> {
    return mapOf(
        metadata_tidspunkt to ofInstant(tidspunkt, localTimeZone).format(dateTimeFormater),
        metadata_kilde to kilde,
        metadata_aarsak to aarsak,
        metadata_brukertype to brukertype
    )
}
package no.nav.paw.bqadapter.bigquery.schema.structs

import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.StandardSQLTypeName.DATE
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import no.nav.paw.bqadapter.bigquery.schema.ofRequiredType
import java.time.Instant
import java.time.LocalDate.ofInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val metadata_tidspunkt = "tidspunkt"
private const val metadata_kilde = "kilde"
private const val metadata_aarsak = "aarsak"
private const val metadata_brukertype = "brukertype"
private val localTimeZone = ZoneId.of("Europe/Oslo")
private val dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

val metadataStruct get(): FieldList = FieldList.of(
    metadata_tidspunkt.ofRequiredType(DATE),
    metadata_kilde.ofRequiredType(STRING),
    metadata_aarsak.ofRequiredType(STRING),
    metadata_brukertype.ofRequiredType(STRING)
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

package no.nav.paw.bqadapter.bigquery.schema.structs

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.StandardSQLTypeName
import java.time.Instant

private const val metadata_tidspunkt = "tidspunkt"
private const val metadata_kilde = "kilde"
private const val metadata_aarsak = "aarsak"
private const val metadata_brukertype = "brukertype"

val metadataStruct get(): FieldList = FieldList.of(
    Field.of(metadata_tidspunkt, StandardSQLTypeName.TIMESTAMP),
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
        metadata_tidspunkt to tidspunkt,
        metadata_kilde to kilde,
        metadata_aarsak to aarsak,
        metadata_brukertype to brukertype
    )
}
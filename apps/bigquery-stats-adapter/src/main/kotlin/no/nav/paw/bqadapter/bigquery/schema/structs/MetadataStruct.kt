package no.nav.paw.bqadapter.bigquery.schema.structs

import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.StandardSQLTypeName.DATE
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import no.nav.paw.bqadapter.bigquery.schema.ofRequiredType
import no.nav.paw.bqadapter.bigquery.schema.toBqDateString
import java.time.Instant

private const val metadata_tidspunkt = "tidspunkt"
private const val metadata_kilde = "kilde"
private const val metadata_aarsak = "aarsak"
private const val metadata_brukertype = "brukertype"

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
        metadata_tidspunkt to tidspunkt.toBqDateString(),
        metadata_kilde to kilde,
        metadata_aarsak to aarsak,
        metadata_brukertype to brukertype
    )
}

package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field.of
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import no.nav.paw.arbeidssokerregisteret.intern.v1.HarOpplysninger
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.bqadapter.Encoder
import no.nav.paw.bqadapter.bigquery.schema.structs.metadataStruct

private const val hendelser_hendelse_id = "correlation_id"
private const val hendelser_ident = "id"
private const val hendelser_type = "type"
private const val hendelser_metadata = "metadata"
private const val hendelser_options = "options"

val hendelserSchema: Schema
    get() = Schema.of(
        hendelser_ident.ofRequiredType(STRING),
        hendelser_hendelse_id.ofRequiredType(STRING),
        hendelser_type.ofRequiredType(STRING),
        of(hendelser_metadata, StandardSQLTypeName.STRUCT, metadataStruct),
        hendelser_options.ofRepeatedType(STRING)
    )

fun hendelseRad(
    encoder: Encoder,
    hendelse: Hendelse
): Map<String, Any> {
    val maskertHendelseId = encoder.encodePeriodeId(hendelse.hendelseId)
    val maskertIdent = encoder.encodeArbeidssoekerId(hendelse.id)
    return mapOf(
        hendelser_ident to maskertIdent,
        hendelser_hendelse_id to maskertHendelseId,
        hendelser_type to hendelse.hendelseType,
        hendelser_metadata to metadataStruct(
            tidspunkt = hendelse.metadata.tidspunkt,
            kilde = hendelse.metadata.kilde,
            aarsak = hendelse.metadata.aarsak,
            brukertype = hendelse.metadata.utfoertAv.type.name.lowercase()
        ),
        hendelser_options to ((hendelse as? HarOpplysninger)?.opplysninger?.toList()?.map { it.name.lowercase() } ?: emptyList())
    )
}

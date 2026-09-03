package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import com.google.cloud.bigquery.StandardSQLTypeName.STRUCT
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.bqadapter.Encoder
import no.nav.paw.bqadapter.bigquery.schema.structs.metadataStruct

private const val correlation_id = "correlation_id"
private const val sendt_inn_av = "sendt_inn_av"
private const val profilert_til = "profilert_til"
private const val egenvurdering_felt = "egenvurdering"

val egenvurderingSchema: Schema
    get() = Schema.of(
        correlation_id.ofRequiredType(STRING),
        Field.newBuilder(sendt_inn_av, STRUCT, metadataStruct)
            .setMode(Field.Mode.REQUIRED)
            .build(),
        profilert_til.ofRequiredType(STRING),
        egenvurdering_felt.ofRequiredType(STRING)
    )

fun egenvurderingRad(
    encoder: Encoder,
    egenvurdering: Egenvurdering
): Map<String, Any> {
    val metadata = egenvurdering.sendtInnAv
    return mapOf(
        correlation_id to encoder.encodePeriodeId(egenvurdering.periodeId),
        sendt_inn_av to metadataStruct(
            tidspunkt = metadata.tidspunkt,
            kilde = metadata.kilde,
            aarsak = metadata.aarsak,
            brukertype = metadata.utfoertAv.type.name.lowercase()
        ),
        profilert_til to egenvurdering.profilertTil.name.lowercase(),
        egenvurdering_felt to egenvurdering.egenvurdering.name.lowercase()
    )
}

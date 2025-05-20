package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bqadapter.Encoder
import no.nav.paw.bqadapter.bigquery.schema.structs.metadataStruct

private const val perioder_correlation_id = "correlation_id"
private const val perioder_startet = "startet"
private const val perioder_avsluttet = "avsluttet"

val perioderSchema: Schema
    get() = Schema.of(
        Field.of(perioder_correlation_id, StandardSQLTypeName.STRING),
        Field.of(perioder_startet, StandardSQLTypeName.STRUCT, metadataStruct),
        Field.of(perioder_avsluttet, StandardSQLTypeName.STRUCT, metadataStruct)
    )

fun periodeRad(encoder: Encoder, periode: Periode): Map<String, Any> {
    val maskertPeriodeId = encoder.encodePeriodeId(periode.id)
    return mapOf(
        perioder_correlation_id to maskertPeriodeId,
        perioder_startet to metadataStruct(
            tidspunkt = periode.startet.tidspunkt,
            kilde = periode.startet.kilde,
            aarsak = periode.startet.aarsak,
            brukertype = periode.startet.utfoertAv.type.name.lowercase()
        )
    ).let { map ->
        if (periode.avsluttet != null) {
            map + mapOf(
                perioder_avsluttet to metadataStruct(
                    tidspunkt = periode.avsluttet.tidspunkt,
                    kilde = periode.avsluttet.kilde,
                    aarsak = periode.avsluttet.aarsak,
                    brukertype = periode.avsluttet.utfoertAv.type.name.lowercase()
                )
            )
        } else {
            map
        }
    }
}
package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName.BOOL
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import com.google.cloud.bigquery.StandardSQLTypeName.STRUCT
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.bqadapter.Encoder
import no.nav.paw.bqadapter.bigquery.schema.structs.metadataStruct

private const val correlation_id = "correlation_id"
private const val opplysninger_id = "opplysninger_id"
private const val sendt_inn_av = "sendt_inn_av"
private const val profilert_til = "profilert_til"
private const val jobbet_sammenhengende_seks_av_tolv_siste_mnd =
    "jobbet_sammenhengende_seks_av_tolv_siste_mnd"
private const val aldersgruppe = "aldersgruppe"

val profileringSchema: Schema
    get() = Schema.of(
        correlation_id.ofRequiredType(STRING),
        opplysninger_id.ofRequiredType(STRING),
        Field.newBuilder(sendt_inn_av, STRUCT, metadataStruct)
            .setMode(Field.Mode.REQUIRED)
            .build(),
        profilert_til.ofRequiredType(STRING),
        jobbet_sammenhengende_seks_av_tolv_siste_mnd.ofRequiredType(BOOL),
        aldersgruppe.ofRequiredType(STRING)
    )

fun profileringRad(
    encoder: Encoder,
    profilering: Profilering
): Map<String, Any> {
    val metadata = profilering.sendtInnAv
    return mapOf(
        correlation_id to encoder.encodePeriodeId(profilering.periodeId),
        opplysninger_id to encoder.encodeOpplysningsId(profilering.opplysningerOmArbeidssokerId),
        sendt_inn_av to metadataStruct(
            tidspunkt = metadata.tidspunkt,
            kilde = metadata.kilde,
            aarsak = metadata.aarsak,
            brukertype = metadata.utfoertAv.type.name.lowercase()
        ),
        profilert_til to profilering.profilertTil.name.lowercase(),
        jobbet_sammenhengende_seks_av_tolv_siste_mnd to
                profilering.jobbetSammenhengendeSeksAvTolvSisteMnd,
        aldersgruppe to aldersgruppe(profilering.alder)
    )
}

private fun aldersgruppe(alder: Int?): String = when {
    alder == null -> "ukjent"
    alder < 0 -> "ugyldig"
    alder < 30 -> "under_30"
    alder < 40 -> "30_39"
    alder < 50 -> "40_49"
    alder < 60 -> "50_59"
    else -> "60_pluss"
}

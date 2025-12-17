package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.StandardSQLTypeName.BOOL
import com.google.cloud.bigquery.StandardSQLTypeName.DATE
import com.google.cloud.bigquery.StandardSQLTypeName.INT64
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.bqadapter.Encoder
import java.time.Duration.ofMillis
import java.time.Instant

private const val perioder_correlation_id = "correlation_id"
private const val tidspunkt = "tidspunkt"
private const val loesning = "loesning"
private const val grace_periode_dager_field = "grace_periode_dager"
private const val intervall_dager_field = "intervall_dager"
private const val handling = "handling"
private const val frist_brutt = "frist_brutt"
private const val insert_tidspunkt = "insert_tidspunkt"
private const val batch_order = "batch_order"

val paaVegnaAvSchema: Schema
    get() = Schema.of(
        perioder_correlation_id.ofRequiredType(STRING),
        tidspunkt.ofRequiredType(DATE),
        loesning.ofRequiredType(STRING),
        handling.ofRequiredType(STRING),
        frist_brutt.ofOptionalType(BOOL),
        insert_tidspunkt.ofRequiredType(INT64),
        batch_order.ofRequiredType(INT64),
        Field.of(grace_periode_dager_field, INT64),
        Field.of(intervall_dager_field, INT64)
    )

fun påVegneAvRad(
    encoder: Encoder,
    recordTimestamp: Instant,
    paaVegneAv: PaaVegneAv,
    batchOrder: Long
): Map<String, Any> {
    val maskertPeriodeId = encoder.encodePeriodeId(paaVegneAv.periodeId)
    return when (val paaVegnaAvHandling = paaVegneAv.handling) {
        is Start -> mapOf(
            perioder_correlation_id to maskertPeriodeId,
            tidspunkt to recordTimestamp.toBqDateString(),
            loesning to paaVegneAv.bekreftelsesloesning.name.lowercase(),
            grace_periode_dager_field to ofMillis(paaVegnaAvHandling.graceMS).toDays(),
            intervall_dager_field to ofMillis(paaVegnaAvHandling.intervalMS).toDays(),
            batch_order to batchOrder,
            insert_tidspunkt to System.currentTimeMillis(),
            handling to "start"
        )

        is Stopp -> mapOf(
            perioder_correlation_id to maskertPeriodeId,
            tidspunkt to recordTimestamp.toBqDateString(),
            loesning to paaVegneAv.bekreftelsesloesning.name.lowercase(),
            handling to "stopp",
            batch_order to batchOrder,
            insert_tidspunkt to System.currentTimeMillis(),
            frist_brutt to paaVegnaAvHandling.fristBrutt
        )
        else -> throw IllegalArgumentException("Unknown PaaVegneAv handling: ${paaVegnaAvHandling::class.qualifiedName}")
    }
}

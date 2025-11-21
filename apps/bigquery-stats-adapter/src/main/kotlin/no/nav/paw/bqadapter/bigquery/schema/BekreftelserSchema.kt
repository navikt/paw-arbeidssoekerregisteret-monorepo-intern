package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName.BOOL
import com.google.cloud.bigquery.StandardSQLTypeName.DATE
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bqadapter.Encoder

private const val perioder_correlation_id = "correlation_id"
private const val tidspunkt = "tidspunkt"
private const val loesning = "loesning"
private const val gjelder_fra = "gjelder_fra"
private const val gjelder_til = "gjelder_til"
private const val har_jobbet = "har_jobbet"
private const val vil_fortsette = "vil_fortsette"
private const val brukertype = "brukertype"

val bekreftelseSchema: Schema
    get() = Schema.of(
        perioder_correlation_id.ofRequiredType(STRING),
        tidspunkt.ofRequiredType(DATE),
        loesning.ofRequiredType(STRING),
        brukertype.ofRequiredType(STRING),
        gjelder_fra.ofRequiredType(DATE),
        gjelder_til.ofRequiredType(DATE),
        har_jobbet.ofRequiredType(BOOL),
        vil_fortsette.ofRequiredType(BOOL)
    )

fun bekreftelseRad(
    encoder: Encoder,
    bekreftelse: Bekreftelse
): Map<String, Any> {
    val maskertPeriodeId = encoder.encodePeriodeId(bekreftelse.periodeId)
    return mapOf(
        perioder_correlation_id to maskertPeriodeId,
        tidspunkt to bekreftelse.svar.sendtInnAv.tidspunkt,
        loesning to bekreftelse.bekreftelsesloesning.name,
        gjelder_fra to bekreftelse.svar.gjelderFra.toBqDateString(),
        gjelder_til to bekreftelse.svar.gjelderTil.toBqDateString(),
        har_jobbet to bekreftelse.svar.harJobbetIDennePerioden,
        vil_fortsette to bekreftelse.svar.vilFortsetteSomArbeidssoeker
    )
}

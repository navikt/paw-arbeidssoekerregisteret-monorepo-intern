package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName.DATE
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.EksternGracePeriodeUtloept
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.bqadapter.Encoder

private const val perioder_correlation_id = "correlation_id"
private const val tidspunkt = "tidspunkt"
private const val gjelder_fra = "gjelder_fra"
private const val gjelder_til = "gjelder_til"
private const val bekreftelse_id = "bekreftelse_id"
private const val hendelse = "hendelse"

val bekreftelseHendelseSchema: Schema
    get() = Schema.of(
        perioder_correlation_id.ofRequiredType(STRING),
        tidspunkt.ofRequiredType(DATE),
        gjelder_fra.ofOptionalType(DATE),
        gjelder_til.ofOptionalType(DATE),
        hendelse.ofRequiredType(STRING),
    )

fun bekreftelseHendelseRad(
    encoder: Encoder,
    bekreftelse: BekreftelseHendelse
): Map<String, Any> {
    val maskertPeriodeId = encoder.encodePeriodeId(bekreftelse.periodeId)
    val maskertBekreftelseId = when (bekreftelse) {
        is BekreftelseTilgjengelig -> encoder.encodeBekreftelseId(bekreftelse.bekreftelseId)
        is BaOmAaAvsluttePeriode -> null
        is BekreftelseMeldingMottatt -> encoder.encodeBekreftelseId(bekreftelse.bekreftelseId)
        is BekreftelsePaaVegneAvStartet -> null
        is EksternGracePeriodeUtloept -> null
        is LeveringsfristUtloept -> encoder.encodeBekreftelseId(bekreftelse.bekreftelseId)
        is PeriodeAvsluttet -> null
        is RegisterGracePeriodeGjenstaaendeTid -> encoder.encodeBekreftelseId(bekreftelse.bekreftelseId)
        is RegisterGracePeriodeUtloept -> encoder.encodeBekreftelseId(bekreftelse.bekreftelseId)
        is RegisterGracePeriodeUtloeptEtterEksternInnsamling -> null
    }
    return mapOf(
        perioder_correlation_id to maskertPeriodeId,
        tidspunkt to bekreftelse.hendelseTidspunkt.toBqDateString(),
        bekreftelse_id to maskertBekreftelseId,
        gjelder_fra to (bekreftelse as? BekreftelseTilgjengelig)?.gjelderFra?.toBqDateString(),
        gjelder_til to (bekreftelse as? BekreftelseTilgjengelig)?.gjelderTil?.toBqDateString(),
        hendelse to bekreftelse.hendelseType
    ).filterValues { it != null }.mapValues { it.value!! }
}

package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.app.tilstand.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode
object StartPeriodeOtelHendelser {
    val starter = "startet_ny_periode"
    val nyttTidspunkt = "startet_nytt_tidspunkt"
    val ignorertFeilretting = "startet_feilretting_ignorert"
    val aarsakTilIgnorertKey = stringKey("arrsak_til_ignorert")
}

fun FunctionContext<TilstandV1?, Long>.startPeriode(
    window: Duration,
    hendelse: Startet
): InternTilstandOgApiTilstander {
    val (startetPeriode: Periode?, nyTilstand: TilstandV1, opplysninger: OpplysningerOmArbeidssoeker?) =
        if (tilstand?.gjeldenePeriode != null) {
            if (hendelse.erGyldigFeilrettingAvStartTid()) {
                val nyttTidspunkt = hendelse.metadata.tidspunktFraKilde?.tidspunkt ?: throw IllegalStateException("Tidspunkt fra kilde er null")
                oppdaterTidspunktForEksiterendePeriode(
                    hendelseScope = scope,
                    nyttTidspunkt = nyttTidspunkt,
                    hendelse = hendelse,
                    tilstand = tilstand,
                    gjeldenePeriode = tilstand.gjeldenePeriode
                ).let { (oppdatertPeriode, nyTilstand) ->
                    Triple(oppdatertPeriode, nyTilstand, null)
                }
            } else {
                throw IllegalStateException("Gjeldene periode er ikke null. Kan ikke starte ny periode.")
            }
        } else {
            Span.current().addEvent(StartPeriodeOtelHendelser.starter)
            val (startetPeriode, nyTilstand) = opprettNyPeriode(hendelse, tilstand)
            val opplysninger = tilstand?.sisteOpplysningerOmArbeidssoeker
                ?.takeIf { window.isWithinWindow(it.metadata.tidspunkt, hendelse.metadata.tidspunkt) }
                ?.let { intern ->
                    OpplysningerOmArbeidssoeker(
                        intern.id,
                        startetPeriode.id,
                        intern.metadata.api(),
                        intern.utdanning?.api(),
                        intern.helse?.api(),
                        intern.jobbsituasjon.api(),
                        intern.annet?.api()
                    )
                }
            Triple(startetPeriode, nyTilstand, opplysninger)
        }
    return InternTilstandOgApiTilstander(
        id = scope.id,
        tilstand = nyTilstand,
        nyOpplysningerOmArbeidssoekerTilstand = opplysninger,
        nyPeriodeTilstand = startetPeriode?.let {
            ApiPeriode(
                startetPeriode.id,
                startetPeriode.identitetsnummer,
                startetPeriode.startet.api(),
                startetPeriode.avsluttet?.api()
            )
        }
    )
}

fun oppdaterTidspunktForEksiterendePeriode(
    hendelseScope: HendelseScope<Long>,
    nyttTidspunkt: Instant,
    hendelse: Startet,
    tilstand: TilstandV1,
    gjeldenePeriode: Periode
): Pair<Periode?, TilstandV1> {
    val orginaltStartTidspunkt = gjeldenePeriode.startet.tidspunkt
    if (nyttTidspunkt.isAfter(orginaltStartTidspunkt)) {
        Span.current().addEvent(
            StartPeriodeOtelHendelser.ignorertFeilretting,
            Attributes.of(
                StartPeriodeOtelHendelser.aarsakTilIgnorertKey,
                "nytt_tidspunkt_er_etter_orginalt_tidspunkt"
            )
        )
        return null to tilstand
    } else {
        Span.current().addEvent(StartPeriodeOtelHendelser.nyttTidspunkt)
        val oppdatertPeriode = gjeldenePeriode.copy(
            startet = gjeldenePeriode.startet.copy(
                tidspunktFraKilde = TidspunktFraKilde(
                    tidspunkt = nyttTidspunkt,
                    avviksType = AvviksType.TIDSPUNKT_KORRIGERT
                ),
                utfoertAv = hendelse.metadata.utfoertAv,
                aarsak = hendelse.metadata.aarsak,
                kilde = hendelse.metadata.kilde
            )
        )
        val nyTilstand = tilstand.copy(
            gjeldenePeriode = oppdatertPeriode,
            hendelseScope = hendelseScope
        )
        return oppdatertPeriode to nyTilstand
    }

}

private fun FunctionContext<TilstandV1?, Long>.opprettNyPeriode(
    hendelse: Startet,
    tilstand: TilstandV1?
): Pair<Periode, TilstandV1> {
    val startetPeriode = Periode(
        id = hendelse.hendelseId,
        identitetsnummer = hendelse.identitetsnummer,
        startet = hendelse.metadata,
        avsluttet = null,
        startetVedOffset = scope.offset
    )
    val nyTilstand: TilstandV1 = tilstand?.copy(
        hendelseScope = scope,
        gjeldeneTilstand = GjeldeneTilstand.STARTET,
        gjeldenePeriode = startetPeriode,
        gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
        alleIdentitetsnummer = tilstand.alleIdentitetsnummer + hendelse.identitetsnummer
    )
        ?: TilstandV1(
            hendelseScope = scope,
            gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
            alleIdentitetsnummer = setOf(hendelse.identitetsnummer),
            gjeldeneTilstand = GjeldeneTilstand.STARTET,
            gjeldenePeriode = startetPeriode,
            forrigePeriode = null,
            sisteOpplysningerOmArbeidssoeker = null,
            forrigeOpplysningerOmArbeidssoeker = null
        )
    return startetPeriode to nyTilstand
}

val windowLogger = LoggerFactory.getLogger("window_check")
fun Duration.isWithinWindow(t1: Instant, t2: Instant): Boolean {
    return (Duration.between(t1, t2).abs() <= this)
        .also { windowLogger.debug("Tidsvindu($this), t1: $t1, t2: $t2, resultat: $it") }
}
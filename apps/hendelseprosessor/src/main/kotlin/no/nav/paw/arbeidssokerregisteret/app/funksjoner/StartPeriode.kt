package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.app.tilstand.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as ApiPeriode

context (HendelseScope<Long>)
fun TilstandV1?.startPeriode(window: Duration, hendelse: Startet): InternTilstandOgApiTilstander {
    if (this?.gjeldenePeriode != null) throw IllegalStateException("Gjeldene periode er ikke null. Kan ikke starte ny periode.")
    val startetPeriode = Periode(
        id = hendelse.hendelseId,
        identitetsnummer = hendelse.identitetsnummer,
        startet = hendelse.metadata,
        avsluttet = null,
        startetVedOffset = currentScope().offset
    )
    val tilstand: TilstandV1 = this?.copy(
        hendelseScope = currentScope(),
        gjeldeneTilstand = GjeldeneTilstand.STARTET,
        gjeldenePeriode = startetPeriode,
        gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
        alleIdentitetsnummer = this.alleIdentitetsnummer + hendelse.identitetsnummer
    )
        ?: TilstandV1(
            hendelseScope = currentScope(),
            gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
            alleIdentitetsnummer = setOf(hendelse.identitetsnummer),
            gjeldeneTilstand = GjeldeneTilstand.STARTET,
            gjeldenePeriode = startetPeriode,
            forrigePeriode = null,
            sisteOpplysningerOmArbeidssoeker = null,
            forrigeOpplysningerOmArbeidssoeker = null
        )
    return InternTilstandOgApiTilstander(
        id = id,
        tilstand = tilstand,
        nyOpplysningerOmArbeidssoekerTilstand = this?.sisteOpplysningerOmArbeidssoeker
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
            },
        nyPeriodeTilstand = ApiPeriode(
            startetPeriode.id,
            startetPeriode.identitetsnummer,
            startetPeriode.startet.api(),
            startetPeriode.avsluttet?.api()
        )
    )
}

val windowLogger = LoggerFactory.getLogger("window_check")
fun Duration.isWithinWindow(t1: Instant, t2: Instant): Boolean {
    return (Duration.between(t1, t2).abs() <= this)
        .also { windowLogger.debug("Tidsvindu($this), t1: $t1, t2: $t2, resultat: $it") }
}
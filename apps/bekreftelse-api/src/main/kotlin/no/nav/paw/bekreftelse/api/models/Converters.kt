package no.nav.paw.bekreftelse.api.models

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.security.authentication.model.Bruker
import no.nav.paw.security.authentication.model.Anonym
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authorization.exception.IngenTilgangException
import java.time.Instant

fun Bruker<*>.asBekreftelseBruker(): no.nav.paw.bekreftelse.melding.v1.vo.Bruker {
    return when (this) {
        is Sluttbruker -> no.nav.paw.bekreftelse.melding.v1.vo.Bruker(BrukerType.SLUTTBRUKER, ident.verdi)
        is NavAnsatt -> no.nav.paw.bekreftelse.melding.v1.vo.Bruker(BrukerType.VEILEDER, ident)
        is Anonym -> no.nav.paw.bekreftelse.melding.v1.vo.Bruker(BrukerType.SYSTEM, ident)
        else -> throw IngenTilgangException("Ukjent brukergruppe")
    }
}

fun BekreftelseTilgjengelig.asBekreftelse(
    harJobbetIDennePerioden: Boolean,
    vilFortsetteSomArbeidssoeker: Boolean,
    bruker: no.nav.paw.bekreftelse.melding.v1.vo.Bruker,
    kilde: String,
    aarsak: String,
    bekreftelsesloesning: Bekreftelsesloesning
): Bekreftelse {
    return Bekreftelse.newBuilder()
        .setBekreftelsesloesning(bekreftelsesloesning)
        .setId(bekreftelseId)
        .setPeriodeId(periodeId)
        .setSvar(
            asSvar(
                harJobbetIDennePerioden,
                vilFortsetteSomArbeidssoeker,
                bruker,
                kilde,
                aarsak
            )
        )
        .build()
}

private fun BekreftelseTilgjengelig.asSvar(
    harJobbetIDennePerioden: Boolean,
    vilFortsetteSomArbeidssoeker: Boolean,
    bruker: no.nav.paw.bekreftelse.melding.v1.vo.Bruker,
    kilde: String,
    aarsak: String
): Svar {
    return Svar.newBuilder()
        .setSendtInnAv(
            Metadata.newBuilder()
                .setUtfoertAv(bruker)
                .setKilde(kilde)
                .setAarsak(aarsak)
                .setTidspunkt(Instant.now())
                .build()
        )
        .setGjelderFra(gjelderFra)
        .setGjelderTil(gjelderTil)
        .setHarJobbetIDennePerioden(harJobbetIDennePerioden)
        .setVilFortsetteSomArbeidssoeker(vilFortsetteSomArbeidssoeker)
        .build()
}

fun BekreftelseRow.asTilgjengeligBekreftelse() = this.data.asTilgjengeligBekreftelse()

private fun BekreftelseTilgjengelig.asTilgjengeligBekreftelse() = TilgjengeligBekreftelse(
    periodeId = this.periodeId,
    bekreftelseId = this.bekreftelseId,
    gjelderFra = this.gjelderFra,
    gjelderTil = this.gjelderTil
)

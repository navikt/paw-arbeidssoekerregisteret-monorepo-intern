package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import java.time.Instant
import java.util.*

data class MottaBekreftelseRequest(
    val identitetsnummer: String?,
    val bekreftelseId: UUID,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)

fun BekreftelseTilgjengelig.asBekreftelse(
    harJobbetIDennePerioden: Boolean,
    vilFortsetteSomArbeidssoeker: Boolean,
    bruker: Bruker,
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
    bruker: Bruker,
    kilde: String,
    aarsak: String
): Svar {
    return Svar.newBuilder()
        .setSendtInn(
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
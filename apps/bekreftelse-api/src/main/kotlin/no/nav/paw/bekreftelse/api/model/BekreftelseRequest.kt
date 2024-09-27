package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appImageOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import java.time.Instant
import java.util.*

data class BekreftelseRequest(
    val identitetsnummer: String?,
    val bekreftelseId: UUID,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)

fun BekreftelseRequest.asApi(
    periodeId: UUID,
    gjelderFra: Instant,
    gjelderTil: Instant,
    innloggetBruker: InnloggetBruker
): Bekreftelse {
    val runtimeEnvironment = currentRuntimeEnvironment
    return Bekreftelse.newBuilder()
        .setNamespace(runtimeEnvironment.namespaceOrDefaultForLocal())
        .setId(bekreftelseId)
        .setPeriodeId(periodeId)
        .setSvar(asApi(gjelderFra, gjelderTil, innloggetBruker, runtimeEnvironment))
        .build()
}

private fun BekreftelseRequest.asApi(
    gjelderFra: Instant,
    gjelderTil: Instant,
    innloggetBruker: InnloggetBruker,
    runtimeEnvironment: RuntimeEnvironment
): Svar {
    return Svar.newBuilder()
        .setSendtInn(
            Metadata.newBuilder()
                .setUtfoertAv(innloggetBruker.asApi())
                .setKilde(runtimeEnvironment.appImageOrDefaultForLocal())
                .setAarsak("Mottatt bekreftelse") // TODO Hva skal dette være
                .setTidspunkt(Instant.now())
                .build()
        )
        .setGjelderFra(gjelderFra)
        .setGjelderTil(gjelderTil)
        .setHarJobbetIDennePerioden(harJobbetIDennePerioden)
        .setVilFortsetteSomArbeidssoeker(vilFortsetteSomArbeidssoeker)
        .build()
}
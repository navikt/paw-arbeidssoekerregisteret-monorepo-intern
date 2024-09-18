package no.nav.paw.bekreftelse.api.model

import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.config.env.currentAppName
import java.time.Instant
import java.util.*

data class BekreftelseRequest(
    val identitetsnummer: String?,
    val bekreftelseId: UUID,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)

fun BekreftelseRequest.toHendelse(
    periodeId: UUID,
    gjelderFra: Instant,
    gjelderTil: Instant,
    brukerId: String,
    brukerType: BrukerType
) = Bekreftelse.newBuilder()
    .setNamespace("paw") // TODO Hente fra config?
    .setId(bekreftelseId)
    .setPeriodeId(periodeId)
    .setSvar(
        Svar.newBuilder()
            .setSendtInn(
                Metadata.newBuilder()
                    .setUtfoertAv(
                        Bruker.newBuilder()
                            .setId(brukerId)
                            .setType(brukerType)
                            .build()
                    )
                    .setKilde(currentAppName)
                    .setTidspunkt(Instant.now())
                    .build()
            )
            .setGjelderFra(gjelderFra)
            .setGjelderTil(gjelderTil)
            .setHarJobbetIDennePerioden(harJobbetIDennePerioden)
            .setVilFortsetteSomArbeidssoeker(vilFortsetteSomArbeidssoeker)
            .build()
    )
    .build()
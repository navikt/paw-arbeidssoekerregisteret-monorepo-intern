package no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse

import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import java.time.Instant
import java.util.*

fun bekreftelseMelding(
    id: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    namespace: String = "paw",
    gjelderFra: Instant = Instant.now(),
    gjelderTil: Instant = Instant.now(),
    harJobbetIDennePerioden: Boolean = true,
    vilFortsetteSomArbeidssoeker: Boolean = true
) =
    Bekreftelse
        .newBuilder()
        .setPeriodeId(periodeId)
        .setNamespace(namespace)
        .setId(id)
        .setSvar(
            Svar
            .newBuilder()
            .setSendtInn(
                Metadata
                    .newBuilder()
                    .setTidspunkt(Instant.now())
                    .setUtfoertAv(
                        Bruker
                            .newBuilder()
                            .setId("test")
                            .setType(BrukerType.SLUTTBRUKER)
                            .build()
                    ).setKilde("test")
                    .setAarsak("test")
                    .build()
            )
            .setGjelderFra(gjelderFra)
            .setGjelderTil(gjelderTil)
            .setHarJobbetIDennePerioden(harJobbetIDennePerioden)
            .setVilFortsetteSomArbeidssoeker(vilFortsetteSomArbeidssoeker)
            .build())
        .build()
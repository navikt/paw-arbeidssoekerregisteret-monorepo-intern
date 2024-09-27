package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand
import java.time.Instant
import java.util.*


class BekreftelseMeldingTopologyTest : FreeSpec({

    val identitetsnummer = "12345678901"
    val startTime = Instant.ofEpochMilli(1704185347000)

    "For melding mottatt uten en tilhørende tilstand skal tilstand være uendret og hendelselogg skal være tom" {
        with(ApplicationTestContext()) {

            val bekreftelseMelding = bekreftelseMelding(
                periodeId = UUID.randomUUID(),
                namespace = "tull",
                gjelderFra = Instant.now(),
                gjelderTil = Instant.now(),
                harJobbetIDennePerioden = true,
                vilFortsetteSomArbeidssoeker = true
            )

            bekreftelseTopic.pipeInput(1234L, bekreftelseMelding)
            val stateStore =
                testDriver.getKeyValueStore<UUID, InternTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
            stateStore.all().asSequence().toList().size shouldBe 0

            hendelseLoggTopicOut.isEmpty shouldBe true
        }
    }

    "Mottatt melding med tilhørende tilstand av typen VenterSvar skal oppdatere tilstand til Levert og sende BekreftelseMeldingMottatt hendelse" {
        with(ApplicationTestContext(startTime)) {
            val (periode, kafkaKeyResponse) = periode(identitetsnummer = identitetsnummer, startet = startTime)

            periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
            testDriver.advanceWallClockTime(
                BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset)
                    .plusSeconds(5)
            )
            val bekreftelseId = (hendelseLoggTopicOut.readKeyValue().value as BekreftelseTilgjengelig).bekreftelseId
            val bekreftelseMelding = bekreftelseMelding(
                id = bekreftelseId,
                periodeId = periode.id,
                namespace = "paw",
                gjelderFra = startTime,
                gjelderTil = startTime.plus(BekreftelseConfig.bekreftelseInterval),
                harJobbetIDennePerioden = true,
                vilFortsetteSomArbeidssoeker = true
            )
            bekreftelseTopic.pipeInput(kafkaKeyResponse.key, bekreftelseMelding)

            val stateStore =
                testDriver.getKeyValueStore<UUID, InternTilstand>(applicationConfig.kafkaTopology.internStateStoreName)
            val internTilstand = stateStore[periode.id]

            internTilstand shouldBe InternTilstand(
                periode = PeriodeInfo(
                    periodeId = periode.id,
                    identitetsnummer = periode.identitetsnummer,
                    arbeidsoekerId = kafkaKeyResponse.id,
                    recordKey = kafkaKeyResponse.key,
                    startet = periode.startet.tidspunkt,
                    avsluttet = periode.avsluttet?.tidspunkt
                ),
                bekreftelser = listOf(
                    no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse(
                        tilstand = Tilstand.Levert,
                        tilgjengeliggjort = startTime.plus(
                            BekreftelseConfig.bekreftelseInterval.minus(BekreftelseConfig.bekreftelseTilgjengeligOffset)
                                .plusSeconds(5)
                        ),
                        fristUtloept = null,
                        sisteVarselOmGjenstaaendeGraceTid = null,
                        bekreftelseId = bekreftelseMelding.id,
                        gjelderFra = bekreftelseMelding.svar.gjelderFra,
                        gjelderTil = bekreftelseMelding.svar.gjelderTil
                    )
                )
            )

            hendelseLoggTopicOut.isEmpty shouldBe false
            val hendelse = hendelseLoggTopicOut.readKeyValue()
            hendelse.key shouldBe kafkaKeyResponse.key
            hendelse.value shouldBe BekreftelseMeldingMottatt(
                hendelseId = hendelse.value.hendelseId,
                periodeId = periode.id,
                arbeidssoekerId = kafkaKeyResponse.id,
                bekreftelseId = bekreftelseMelding.id,
                hendelseTidspunkt = hendelse.value.hendelseTidspunkt
            )
        }

    }

})

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
                .build()

        )
        .build()
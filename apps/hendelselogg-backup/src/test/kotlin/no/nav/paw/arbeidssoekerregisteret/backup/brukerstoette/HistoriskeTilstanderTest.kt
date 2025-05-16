package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Snapshot
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Tilstand
import no.nav.paw.arbeidssoekerregisteret.backup.apiHendelse
import no.nav.paw.arbeidssoekerregisteret.backup.avsluttet
import no.nav.paw.arbeidssoekerregisteret.backup.avvist
import no.nav.paw.arbeidssoekerregisteret.backup.opplysninger
import no.nav.paw.arbeidssoekerregisteret.backup.startet
import no.nav.paw.arbeidssoekerregisteret.backup.storedHendelseRecord
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt

class HistoriskeTilstanderTest : FreeSpec({
    "verifiser at historiskeTilstander funksjonen fungerer" - {
        "for tom liste retuneres tom liste" {
            historiskeTilstander(emptyList()) shouldBe emptyList()
        }
        "med en startet hendelse returneres en liste med en tilstand" {
            val startet = startet().storedHendelseRecord(
                offset = 1,
                partition = 3,
                recordKey = 2
            )
            historiskeTilstander(
                listOf(
                    startet
                )
            ) shouldBe listOf(
                Snapshot(
                    hendelse = startet.apiHendelse(),
                    gjeldeneTilstand = null,
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = startet.data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = startet.data.hendelseId,
                        gjeldeneOpplysningsId = null
                    ),
                    endret = true
                )
            )
        }
        "med flere perioder og opplysinger skal vi få alle tilstandene" - {
            val  hendelser = listOf(
                avvist().storedHendelseRecord(offset = 0),
                startet().storedHendelseRecord(offset = 1),
                opplysninger().storedHendelseRecord(offset = 2),
                avsluttet().storedHendelseRecord(offset = 3),
                startet().storedHendelseRecord(offset = 4),
                opplysninger().storedHendelseRecord(offset = 5),
                avsluttet().storedHendelseRecord(offset = 6),
                avvist().storedHendelseRecord(offset = 7)
            )
            val result = historiskeTilstander(hendelser).toList()
            val expected = listOf(
                Snapshot(
                    hendelse = hendelser[0].apiHendelse(),
                    gjeldeneTilstand = null,
                    nyTilstand = null,
                    endret = false
                ),
                Snapshot(
                    hendelse = hendelser[1].apiHendelse(),
                    gjeldeneTilstand = null,
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = hendelser[1].data.hendelseId,
                        gjeldeneOpplysningsId = null
                    ),
                    endret = true
                ),
                Snapshot(
                    hendelse = hendelser[2].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = hendelser[1].data.hendelseId,
                        gjeldeneOpplysningsId = null
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = hendelser[1].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[2].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    endret = true
                ),
                Snapshot(
                    hendelse = hendelser[3].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = hendelser[1].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[2].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[3].data.metadata.tidspunkt,
                        apiKall = null,
                        periodeId = hendelser[1].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[2].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    endret = true
                ),
                Snapshot(
                    hendelse = hendelser[4].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[3].data.metadata.tidspunkt,
                        apiKall = null,
                        periodeId = hendelser[1].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[2].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = hendelser[4].data.hendelseId,
                        gjeldeneOpplysningsId = null
                    ),
                    endret = true
                ),
                Snapshot(
                    hendelse = hendelser[5].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = hendelser[4].data.hendelseId
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = hendelser[4].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[5].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    endret = true
                ),
                Snapshot(
                    hendelse = hendelser[6].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = null,
                        apiKall = null,
                        periodeId = hendelser[4].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[5].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[6].data.metadata.tidspunkt,
                        apiKall = null,
                        periodeId = hendelser[4].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[5].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    endret = true
                ),
                Snapshot(
                    hendelse = hendelser[7].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[6].data.metadata.tidspunkt,
                        apiKall = null,
                        periodeId = hendelser[4].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[5].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[6].data.metadata.tidspunkt,
                        apiKall = null,
                        periodeId = hendelser[4].data.hendelseId,
                        gjeldeneOpplysningsId = (hendelser[5].data as OpplysningerOmArbeidssoekerMottatt)
                            .opplysningerOmArbeidssoeker.id
                    ),
                    endret = false
                )
            )
            result.size shouldBe expected.size
            result.forEachIndexed { index, actual ->
                "Element at index $index(${hendelser[index].data.hendelseType}) should be equal to expected" {
                    actual shouldBe expected[index]
                }
            }
        }
    }
})


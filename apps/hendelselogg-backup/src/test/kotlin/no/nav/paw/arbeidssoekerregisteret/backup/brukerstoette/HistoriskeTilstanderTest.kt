package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.*
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.InnkommendeHendelse
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Tilstand

class HistoriskeTilstanderTest : FreeSpec({
    "verifiser at historiskeTilstander funksjonen fungerer" - {
        "for tom liste retuneres tom liste" {
            historiskeTilstander(emptyList()) shouldBe emptyList()
        }
        "med en startet hendelse returneres en liste med en tilstand" {
            val startet = startet().storedData(
                offset = 1,
                partition = 3,
                recordKey = 2
            )
            historiskeTilstander(
                listOf(
                    startet
                )
            ) shouldBe listOf(
                InnkommendeHendelse(
                    hendelse = startet.apiHendelse(),
                    gjeldeneTilstand = null,
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = startet.data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null
                    )
                )
            )
        }
        "med flere perioder og opplysinger skal vi få alle tilstandene" - {
            val  hendelser = listOf(
                avvist().storedData(offset = 0),
                startet().storedData(offset = 1),
                opplysninger().storedData(offset = 2),
                avsluttet().storedData(offset = 3),
                startet().storedData(offset = 4),
                opplysninger().storedData(offset = 5),
                avsluttet().storedData(offset = 6),
                avvist().storedData(offset = 7)
            )
            val result = historiskeTilstander(hendelser).toList()
            val expected = listOf(
                InnkommendeHendelse(
                    hendelse = hendelser[0].apiHendelse(),
                    gjeldeneTilstand = null,
                    nyTilstand = null
                ),
                InnkommendeHendelse(
                    hendelse = hendelser[1].apiHendelse(),
                    gjeldeneTilstand = null,
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null
                    )
                ),
                InnkommendeHendelse(
                    hendelse = hendelser[2].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = null,
                        apiKall = null
                    )
                ),
                InnkommendeHendelse(
                    hendelse = hendelser[3].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = null,
                        apiKall = null
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[3].data.metadata.tidspunkt,
                        apiKall = null
                    )
                ),
                InnkommendeHendelse(
                    hendelse = hendelser[4].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[1].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[3].data.metadata.tidspunkt,
                        apiKall = null
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null
                    )
                ),
                InnkommendeHendelse(
                    hendelse = hendelser[5].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = false,
                        avsluttet = null,
                        apiKall = null
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = null,
                        apiKall = null
                    )
                ),
                InnkommendeHendelse(
                    hendelse = hendelser[6].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = true,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = null,
                        apiKall = null
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[6].data.metadata.tidspunkt,
                        apiKall = null
                    )
                ),
                InnkommendeHendelse(
                    hendelse = hendelser[7].apiHendelse(),
                    gjeldeneTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[6].data.metadata.tidspunkt,
                        apiKall = null
                    ),
                    nyTilstand = Tilstand(
                        harAktivePeriode = false,
                        startet = hendelser[4].data.metadata.tidspunkt,
                        harOpplysningerMottattHendelse = true,
                        avsluttet = hendelser[6].data.metadata.tidspunkt,
                        apiKall = null
                    )
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


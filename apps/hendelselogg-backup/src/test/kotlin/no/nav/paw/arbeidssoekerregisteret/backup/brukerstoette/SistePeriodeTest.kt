package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Tilstand
import no.nav.paw.arbeidssoekerregisteret.backup.avsluttet
import no.nav.paw.arbeidssoekerregisteret.backup.opplysninger
import no.nav.paw.arbeidssoekerregisteret.backup.startet
import no.nav.paw.arbeidssoekerregisteret.backup.storedData

class SistePeriodeTest: FreeSpec({
    "verifiser at sistePeriode funksjonen fungerer" - {
        "for tom liste retuneres null" {
            sistePeriode(emptyList()) shouldBe null
        }
        "uten startet hendelse returneres null" {
            sistePeriode(
                listOf(
                    avsluttet().storedData(offset = 1),
                    opplysninger().storedData(offset = 2)
                )
            ) shouldBe null
        }
        "med en startet hendelse returneres den som siste periode" {
            val startet = startet()
            sistePeriode(
                listOf(
                    startet.storedData(offset = 1)
                )
            ) shouldBe Tilstand(
                harAktivePeriode = true,
                startet = startet.metadata.tidspunkt,
                harOpplysningerMottattHendelse = false,
                avsluttet = null,
                apiKall = null,
                periodeId = startet.hendelseId
            )
        }
        "med flere perioder returneres den siste" {
            val nyesteperiode = startet()
            val avsluttet = avsluttet()
            val gjeldeneOpplysninger = opplysninger()
            sistePeriode(
                listOf(
                    startet().storedData(offset = 1),
                    opplysninger().storedData(offset = 2),
                    avsluttet().storedData(offset = 3),
                    nyesteperiode.storedData(offset = 4),
                    startet().storedData(offset = 5),
                    gjeldeneOpplysninger.storedData(offset = 6),
                    avsluttet.storedData(offset = 7)
                )
            ) shouldBe Tilstand(
                harAktivePeriode = false,
                startet = nyesteperiode.metadata.tidspunkt,
                harOpplysningerMottattHendelse = true,
                avsluttet = avsluttet.metadata.tidspunkt,
                apiKall = null,
                periodeId = nyesteperiode.hendelseId,
                gjeldeneOpplysningsId = gjeldeneOpplysninger.opplysningerOmArbeidssoeker.id
            )
        }
    }
})


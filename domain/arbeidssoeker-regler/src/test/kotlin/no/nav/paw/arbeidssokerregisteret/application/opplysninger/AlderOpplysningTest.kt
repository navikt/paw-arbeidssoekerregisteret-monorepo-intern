package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.ErOver18Aar
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.ErUnder18Aar
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.UkjentFoedselsaar
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.UkjentFoedselsdato
import no.nav.paw.pdl.graphql.generated.hentperson.Foedselsdato
import no.nav.paw.pdl.graphql.generated.hentperson.Metadata

class AlderOpplysningTest : FreeSpec({
    "alderOpplysning for flere foedselsdatoer" - {
        "returnerer foerste resultat naar alle er over 18" {
            val foerste = foedselsdato(foedselsdato = null, foedselsaar = 1980)
            val andre = foedselsdato(foedselsdato = "1970-01-01", foedselsaar = 1970)

            alderOpplysning(listOf(foerste, andre)) shouldBe
                    setOf(UkjentFoedselsdato, ErOver18Aar)
        }

        "returnerer under 18 naar minst en er under 18 og de andre er ukjente" {
            val ukjentFoerste = foedselsdato(foedselsdato = null, foedselsaar = null)
            val under18 = foedselsdato(foedselsdato = null, foedselsaar = 2015)
            val ukjentAndre = foedselsdato(foedselsdato = null, foedselsaar = null)

            alderOpplysning(listOf(ukjentFoerste, under18, ukjentAndre)) shouldBe
                setOf(UkjentFoedselsdato, ErUnder18Aar)
        }

        "returnerer et resultat uten under/over 18 naar ingen har alderstreff" {
            val over18 = foedselsdato(foedselsdato = "1985-06-20", foedselsaar = 1985)
            val ukjent = foedselsdato(foedselsdato = null, foedselsaar = null)

            alderOpplysning(listOf(over18, ukjent)) shouldBe setOf(UkjentFoedselsdato, UkjentFoedselsaar)
        }

        "returnerer ukjent foedselsdato og foedselsaar for tom liste" {
            alderOpplysning(emptyList()) shouldBe setOf(UkjentFoedselsaar, UkjentFoedselsdato)
        }
    }

    "alderOpplysning konsistens for 0 eller 1 foedselsdato" - {
        "tom liste gir samme resultat som null" {
            alderOpplysning(emptyList()) shouldBe alderOpplysning(null)
        }

        "enkeltelement gir samme resultat som direkte kall" {
            val testdata = listOf(
                foedselsdato(foedselsdato = null, foedselsaar = null),
                foedselsdato(foedselsdato = null, foedselsaar = 1980),
                foedselsdato(foedselsdato = null, foedselsaar = 2015),
                foedselsdato(foedselsdato = "1988-03-15", foedselsaar = 1988),
                foedselsdato(foedselsdato = "2012-11-02", foedselsaar = 2012)
            )

            testdata.forEach { fd ->
                alderOpplysning(listOf(fd)) shouldBe alderOpplysning(fd)
            }
        }
    }
})

private fun foedselsdato(foedselsdato: String?, foedselsaar: Int?) =
    Foedselsdato(
        foedselsdato = foedselsdato,
        foedselsaar = foedselsaar,
        metadata = Metadata("FREG")
    )


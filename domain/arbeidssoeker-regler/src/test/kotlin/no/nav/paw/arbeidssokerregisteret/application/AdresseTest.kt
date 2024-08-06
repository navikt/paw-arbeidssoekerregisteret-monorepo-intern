package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.adreseOpplysning
import no.nav.paw.pdl.graphql.generated.hentperson.*

class AdresseTest : FreeSpec({
    "Test for adresse" - {
        "Test for norske adresser" - {
            "har veiadresse" {
                val veiadresse = Bostedsadresse(
                    vegadresse = Vegadresse(
                        kommunenummer = "1201"
                    )
                )
                adreseOpplysning(veiadresse) shouldBe setOf(HarNorskAdresse, HarRegistrertAdresseIEuEoes)
            }
            "har matrikkeladresse" {
                val matrikkelAdresse = Bostedsadresse(
                    matrikkeladresse = Matrikkeladresse(
                        kommunenummer = "1201"
                    )
                )
                adreseOpplysning(matrikkelAdresse) shouldBe setOf(HarNorskAdresse, HarRegistrertAdresseIEuEoes)
            }
            "har ukjentBosted" {
                val ukjentBosted = Bostedsadresse(
                    ukjentBosted = UkjentBosted(
                        bostedskommune = "1201"
                    )
                )
                adreseOpplysning(ukjentBosted) shouldBe setOf(HarNorskAdresse, HarRegistrertAdresseIEuEoes)
            }
        }
        "Test for utenlandsk adresse" - {
            "har utenlandsk adresse i EU/EØS" {
                val utenlandskAdresse = Bostedsadresse(
                    utenlandskAdresse = UtenlandskAdresse(
                        landkode = "SWE"
                    )
                )
                adreseOpplysning(utenlandskAdresse) shouldBe setOf(
                    HarUtenlandskAdresse,
                    HarRegistrertAdresseIEuEoes
                )
            }
            "har utenlandsk adresse utenfor EU/EØS" {
                val utenlandskAdresse = Bostedsadresse(
                    utenlandskAdresse = UtenlandskAdresse(
                        landkode = "USA"
                    )
                )
                adreseOpplysning(utenlandskAdresse) shouldBe setOf(HarUtenlandskAdresse)
            }
        }
        "Test for ingen adresse" {
            adreseOpplysning(null) shouldBe setOf(IngenAdresseFunnet)
        }
    }
})
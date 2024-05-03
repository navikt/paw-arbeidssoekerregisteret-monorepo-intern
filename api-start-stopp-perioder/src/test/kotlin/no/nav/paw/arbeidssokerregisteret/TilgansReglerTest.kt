package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.regler.tilgangsReglerIPrioritertRekkefolge
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.pdl.graphql.generated.hentperson.Foedsel
import no.nav.paw.pdl.graphql.generated.hentperson.Person

class TilgansReglerTest : FreeSpec({
    "eval av tilgang skal gi" - {
        "ugydlig request ved IKKE_ANSATT kombinert med GODKJENT_AV_ANSATT" {
            tilgangsReglerIPrioritertRekkefolge.evaluer(
                setOf(
                    Opplysning.IKKE_ANSATT,
                    Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                )
            ).shouldBeInstanceOf<UgyldigRequestBasertPaaAutentisering>()
        }
        "lovlig kombinasjon av ANSATT og FORHANDSGODKJENT_AV_ANSATT" {
            tilgangsReglerIPrioritertRekkefolge.evaluer(
                setOf(
                    Opplysning.ANSATT_TILGANG,
                    Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                )
            ).shouldBeInstanceOf<TilgangOK>()
        }
    }
}
)

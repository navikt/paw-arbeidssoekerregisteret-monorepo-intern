package no.nav.paw.arbeidssokerregisteret

import arrow.core.Either
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.AnsattTilgang
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.IkkeAnsatt
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.regler.AnsattHarTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.application.regler.IkkeAnsattOgForhaandsgodkjentAvAnsatt
import no.nav.paw.arbeidssokerregisteret.application.regler.tilgangsReglerIPrioritertRekkefolge

class TilgansReglerTest : FreeSpec({
    "eval av tilgang skal gi" - {
        "ugydlig request ved IKKE_ANSATT kombinert med GODKJENT_AV_ANSATT" {
            val resultat = tilgangsReglerIPrioritertRekkefolge.evaluer(
                setOf(
                    IkkeAnsatt,
                    DomeneOpplysning.ErForhaandsgodkjent
                )
            ).shouldBeInstanceOf<Either.Left<Problem>>()
            resultat.value.regel.id shouldBe IkkeAnsattOgForhaandsgodkjentAvAnsatt
            resultat.value.opplysning shouldContainAll listOf(IkkeAnsatt, DomeneOpplysning.ErForhaandsgodkjent)
        }
        "lovlig kombinasjon av ANSATT og FORHANDSGODKJENT_AV_ANSATT" {
            val resultat = tilgangsReglerIPrioritertRekkefolge.evaluer(
                setOf(
                    AnsattTilgang,
                    DomeneOpplysning.ErForhaandsgodkjent
                )
            ).shouldBeInstanceOf<Either.Right<OK>>()
            resultat.value.regel.id shouldBe AnsattHarTilgangTilBruker
            resultat.value.opplysning shouldContainAll listOf(AnsattTilgang, DomeneOpplysning.ErForhaandsgodkjent)
        }
    }
}
)

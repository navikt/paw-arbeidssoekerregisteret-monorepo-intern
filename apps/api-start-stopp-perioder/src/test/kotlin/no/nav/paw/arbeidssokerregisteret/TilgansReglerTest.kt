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
import no.nav.paw.arbeidssokerregisteret.application.regler.*
import no.nav.paw.collections.PawNonEmptyList

class TilgansReglerTest : FreeSpec({
    "eval av tilgang skal gi" - {
        "ugydlig request ved IKKE_ANSATT kombinert med GODKJENT_AV_ANSATT" {
            val resultat = TilgangsRegler.evaluer(
                setOf(
                    IkkeAnsatt,
                    DomeneOpplysning.ErForhaandsgodkjent
                )
            ).shouldBeInstanceOf<Either.Left<PawNonEmptyList<Problem>>>()
            resultat.value.first.regel.id shouldBe IkkeAnsattOgForhaandsgodkjentAvAnsatt
            resultat.value.first.opplysninger shouldContainAll listOf(IkkeAnsatt, DomeneOpplysning.ErForhaandsgodkjent)
        }
        "lovlig kombinasjon av ANSATT og FORHANDSGODKJENT_AV_ANSATT" {
            val resultat = TilgangsRegler.evaluer(
                setOf(
                    AnsattTilgang,
                    DomeneOpplysning.ErForhaandsgodkjent
                )
            ).shouldBeInstanceOf<Either.Right<GrunnlagForGodkjenning>>()
            resultat.value.regel.id shouldBe AnsattHarTilgangTilBruker
            resultat.value.opplysning shouldContainAll listOf(AnsattTilgang, DomeneOpplysning.ErForhaandsgodkjent)
        }
    }
}
)

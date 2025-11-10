package no.nav.paw.arbeidssokerregisteret

import arrow.core.Either
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.application.GrunnlagForGodkjenning
import no.nav.paw.arbeidssokerregisteret.application.Problem
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning
import no.nav.paw.arbeidssokerregisteret.application.evaluer
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.regler.AnsattHarTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.application.regler.IkkeAnsattOgIkkeSystemOgForhaandsgodkjent
import no.nav.paw.arbeidssokerregisteret.application.regler.SystemHarTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.application.regler.ValideringsRegler
import no.nav.paw.felles.collection.PawNonEmptyList

class TilgansReglerTest : FreeSpec({
    "eval av tilgang skal gi" - {
        "ugydlig request ved IKKE_ANSATT og IKKE_SYSTEM kombinert med GODKJENT_AV_ANSATT" {
            val resultat = ValideringsRegler.evaluer(
                setOf(
                    AuthOpplysning.IkkeAnsatt,
                    AuthOpplysning.IkkeSystem,
                    DomeneOpplysning.ErForhaandsgodkjent
                )
            ).shouldBeInstanceOf<Either.Left<PawNonEmptyList<Problem>>>()
            resultat.value.first.regel.id shouldBe IkkeAnsattOgIkkeSystemOgForhaandsgodkjent
            resultat.value.first.opplysninger shouldContainAll listOf(
                AuthOpplysning.IkkeAnsatt,
                AuthOpplysning.IkkeSystem,
                DomeneOpplysning.ErForhaandsgodkjent
            )
        }
        "lovlig kombinasjon av ANSATT og IKKE_SYSTEM kombinert med FORHANDSGODKJENT_AV_ANSATT" {
            val resultat = ValideringsRegler.evaluer(
                setOf(
                    AuthOpplysning.AnsattTilgang,
                    AuthOpplysning.IkkeSystem,
                    DomeneOpplysning.ErForhaandsgodkjent
                )
            ).shouldBeInstanceOf<Either.Right<GrunnlagForGodkjenning>>()
            resultat.value.regel.id shouldBe AnsattHarTilgangTilBruker
            resultat.value.opplysning shouldContainAll listOf(
                AuthOpplysning.AnsattTilgang,
                AuthOpplysning.IkkeSystem,
                DomeneOpplysning.ErForhaandsgodkjent
            )
        }
        "lovlig kombinasjon av IKKE_ANSATT og SYSTEM kombinert med FORHANDSGODKJENT_AV_ANSATT" {
            val resultat = ValideringsRegler.evaluer(
                setOf(
                    AuthOpplysning.IkkeAnsatt,
                    AuthOpplysning.SystemTilgang,
                    DomeneOpplysning.ErForhaandsgodkjent
                )
            ).shouldBeInstanceOf<Either.Right<GrunnlagForGodkjenning>>()
            resultat.value.regel.id shouldBe SystemHarTilgangTilBruker
            resultat.value.opplysning shouldContainAll listOf(
                AuthOpplysning.IkkeAnsatt,
                AuthOpplysning.SystemTilgang,
                DomeneOpplysning.ErForhaandsgodkjent
            )
        }
    }
}
)

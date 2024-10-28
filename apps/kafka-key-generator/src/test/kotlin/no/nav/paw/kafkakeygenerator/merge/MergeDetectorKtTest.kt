package no.nav.paw.kafkakeygenerator.merge

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.sequences.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class MergeDetectorKtTest : FreeSpec({

    "Når 2 identiteter er samme person i PDL, men har forskjellige arbeidssøkerId skal det slå ut som 'merge detected" {
        val local = mapOf(
            Identitetsnummer("1") to ArbeidssoekerId(11),
            Identitetsnummer("2") to ArbeidssoekerId(12)
        )
        val pdl = mapOf(
            "1" to listOf(
                IdentInformasjon(
                    ident = "1",
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                    historisk = false
                ),
                IdentInformasjon(
                    ident = "2",
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                    historisk = false
                )
            )
        )
        detectMerges(
            local = local,
            pdl = pdl
        ) should { merges ->
            merges.toList().size shouldBe 1
        }
    }

    "Når 2 identiteter er samme person i PDL, og har arbeidssøkerId skal det ikke slå ut som 'merge detected" {
        val local = mapOf(
            Identitetsnummer("1") to ArbeidssoekerId(11),
            Identitetsnummer("2") to ArbeidssoekerId(11)
        )
        val pdl = mapOf(
            "1" to listOf(
                IdentInformasjon(
                    ident = "1",
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                    historisk = false
                ),
                IdentInformasjon(
                    ident = "2",
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                    historisk = false
                )
            )
        )
        detectMerges(
            local = local,
            pdl = pdl
        ).shouldBeEmpty()
    }

})
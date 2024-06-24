package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning as HendelseOpplysning

class RegelExtensionsKtTest : FreeSpec({
    "Alle opplysninger kan konverteres til hendelse-opplysning" - {
        val domeneOpplysninger = DomeneOpplysning::class.sealedSubclasses.mapNotNull { it.objectInstance }.map { it as Opplysning }
        val authOpplysninger = AuthOpplysning::class.sealedSubclasses.mapNotNull { it.objectInstance }.map { it as Opplysning }
        val  hendelseOpplysninger = (domeneOpplysninger + authOpplysninger).map {
            mapToHendelseOpplysning(it)
                .also { res ->
                    "$it skal ikke mappes til ${HendelseOpplysning.UKJENT_OPPLYSNING}" {
                        res.shouldNotBe(HendelseOpplysning.UKJENT_OPPLYSNING)
                    }
                }
        }
        val forventedHendelseOpplysninger = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning.entries
            .filter { it != HendelseOpplysning.UKJENT_OPPLYSNING }
        forventedHendelseOpplysninger.forEach { forventetHendelseOpplysning ->
            "$forventetHendelseOpplysning skal være i resultatet" {
                hendelseOpplysninger shouldContain forventetHendelseOpplysning
            }
        }
        "resultatet skal inneholde ${hendelseOpplysninger.toSet().size} unike opplysninger" {
            forventedHendelseOpplysninger.toSet() shouldBe hendelseOpplysninger.toSet()
        }
    }
})
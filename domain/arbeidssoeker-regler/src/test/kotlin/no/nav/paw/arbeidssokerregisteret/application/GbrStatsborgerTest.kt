package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.ErGbrStatsborger
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.gbrStatsborgerOpplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Metadata
import no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap

class GbrStatsborgerTest : FreeSpec({
    "Test for GBR statsborgere" - {
        "bare et statsborgerskap" {
            val statsborgerskap = Statsborgerskap("GBR", Metadata(emptyList()))
            gbrStatsborgerOpplysning(listOf(statsborgerskap)) shouldContain ErGbrStatsborger
        }
        "flere statsborgerskap" {
            val statsborgerskap0 = Statsborgerskap("NOR", Metadata(emptyList()))
            val statsborgerskap1 = Statsborgerskap("GBR", Metadata(emptyList()))
            val statsborgerskap2 = Statsborgerskap("USA", Metadata(emptyList()))
            gbrStatsborgerOpplysning(
                listOf(
                    statsborgerskap0,
                    statsborgerskap1,
                    statsborgerskap2
                )
            ) shouldContain ErGbrStatsborger
        }
    }
    "Test for ikke GBR statsborgere" - {
        val statsborgerskap = Statsborgerskap("NOR", Metadata(emptyList()))
        "Statsborgerskap: ${statsborgerskap.land} er ikke GBR" {
            gbrStatsborgerOpplysning(listOf(statsborgerskap)).isEmpty()
        }
    }
})

package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.ErNorskStatsborger
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.norskStatsborgerOpplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Metadata
import no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap

class NorskStatsborgerTest : FreeSpec({
    "Test for NOR statsborgere" - {
        "bare et statsborgerskap" {
            val statsborgerskap = Statsborgerskap("NOR", Metadata(emptyList()))
            norskStatsborgerOpplysning(listOf(statsborgerskap)) shouldContain ErNorskStatsborger
        }
        "flere statsborgerskap" {
            val statsborgerskap0 = Statsborgerskap("GBR", Metadata(emptyList()))
            val statsborgerskap1 = Statsborgerskap("NOR", Metadata(emptyList()))
            val statsborgerskap2 = Statsborgerskap("USA", Metadata(emptyList()))
            norskStatsborgerOpplysning(
                listOf(
                    statsborgerskap0,
                    statsborgerskap1,
                    statsborgerskap2
                )
            ) shouldContain ErNorskStatsborger
        }
    }
    "Test for ikke NOR statsborgere" - {
        val statsborgerskap = Statsborgerskap("NOR", Metadata(emptyList()))
        "Statsborgerskap: ${statsborgerskap.land} er ikke NOR" {
            norskStatsborgerOpplysning(listOf(statsborgerskap)).isEmpty()
        }
    }
})

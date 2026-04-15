package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.ErNorskStatsborger
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.norskStatsborgerOpplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Metadata
import no.nav.paw.pdl.graphql.generated.hentperson.Metadata2
import no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap
import kotlin.collections.emptyList

class NorskStatsborgerTest : FreeSpec({
    "Test for NOR statsborgere" - {
        "bare et statsborgerskap" {
            val statsborgerskap = Statsborgerskap("NOR", Metadata2(emptyList()))
            norskStatsborgerOpplysning(listOf(statsborgerskap)) shouldContain ErNorskStatsborger
        }
        "flere statsborgerskap" {
            val statsborgerskap0 = Statsborgerskap("GBR", Metadata2(emptyList()))
            val statsborgerskap1 = Statsborgerskap("NOR", Metadata2(emptyList()))
            val statsborgerskap2 = Statsborgerskap("USA", Metadata2(emptyList()))
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
        val statsborgerskap = Statsborgerskap("NOR", Metadata2(emptyList()))
        "Statsborgerskap: ${statsborgerskap.land} er ikke NOR" {
            norskStatsborgerOpplysning(listOf(statsborgerskap)).isEmpty()
        }
    }
})

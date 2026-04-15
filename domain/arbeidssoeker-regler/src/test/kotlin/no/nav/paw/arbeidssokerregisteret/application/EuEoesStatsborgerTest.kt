package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.ErEuEoesStatsborger
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.euEoesStatsborgerOpplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Metadata
import no.nav.paw.pdl.graphql.generated.hentperson.Metadata2
import no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap

class EuEoesStatsborgerTest : FreeSpec({
    "Test for EU/EØS statsborgere" - {
        alpha3EuEeaCountries
            .map { cuntry -> Statsborgerskap(cuntry, Metadata2(emptyList())) }
            .forEach { statsborgerskap ->
                "Statsborgerskap: ${statsborgerskap.land} er medlem av EU/EØS" {
                    euEoesStatsborgerOpplysning(listOf(statsborgerskap)) shouldContain ErEuEoesStatsborger
                }
            }
        "flere statsborgerskap" {
            val statsborgerskap0 = Statsborgerskap("NOR", Metadata2(emptyList()))
            val statsborgerskap1 = Statsborgerskap("GBR", Metadata2(emptyList()))
            val statsborgerskap2 = Statsborgerskap("USA", Metadata2(emptyList()))
            euEoesStatsborgerOpplysning(listOf(
                statsborgerskap0,
                statsborgerskap1,
                statsborgerskap2
            )) shouldContain ErEuEoesStatsborger
        }
    }
    "Test for ikke EU/EØS statsborgere" - {
        val statsborgerskap = Statsborgerskap("USA", Metadata2(emptyList()))
        "Statsborgerskap: ${statsborgerskap.land} er ikke medlem av EU/EØS" {
            euEoesStatsborgerOpplysning(listOf(statsborgerskap)).isEmpty()
        }
    }
})

val alpha3EuEeaCountries = setOf(
    "AUT", "BEL", "BGR", "HRV", "CZE", "DNK", "EST", "FIN", "FRA",
    "DEU", "GRC", "HUN", "IRL", "ITA", "LVA", "LTU", "LUX", "MLT",
    "NLD", "POL", "PRT", "ROU", "SVK", "SVN", "ESP", "SWE"
)


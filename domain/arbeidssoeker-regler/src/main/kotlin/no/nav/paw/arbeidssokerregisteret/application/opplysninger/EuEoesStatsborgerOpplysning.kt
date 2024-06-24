package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

fun euEoesStatsborgerOpplysning(statsborgerskap: List<Statsborgerskap>): Set<Opplysning> =
    statsborgerskap.map { it.land.uppercase() }
        .any { it in eea }
        .let { return if (it) setOf(ErEuEoesStatsborger) else emptySet() }

val eea = setOf(
    "BEL",
    "BGR",
    "DNK",
    "EST",
    "FIN",
    "FRA",
    "GRC",
    "IRL",
    "ISL",
    "ITA",
    "HRV",
    "CYP",
    "LVA",
    "LIE",
    "LTU",
    "LUX",
    "MLT",
    "NLD",
    "NOR",
    "POL",
    "PRT",
    "ROU",
    "SVK",
    "SVN",
    "ESP",
    "SWE",
    "CZE",
    "DEU",
    "HUN",
    "AUT",
    "CHE"
).map { it.uppercase() }
package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

fun gbrStatsborgerOpplysning(statsborgerskap: List<Statsborgerskap>): Set<Opplysning> =
    statsborgerskap.map { it.land.uppercase() }
        .any { it == "GBR" }
        .let { return if (it) setOf(ErGbrStatsborger) else emptySet() }


package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

fun forenkletFregOpplysning(status: List<Folkeregisterpersonstatus>): Set<Opplysning> =
    status.map { it.forenkletStatus }
        .map { it.uppercase() }
        .map { enkelFolkeRegStatusTilOpplysning[it] ?: UkjentForenkletFregStatus }
        .toSet()

private val enkelFolkeRegStatusTilOpplysning: Map<String, Opplysning> = mapOf(
    "bosattEtterFolkeregisterloven".uppercase() to BosattEtterFregLoven,
    "ikkeBosatt".uppercase() to IkkeBosatt,
    "doedIFolkeregisteret".uppercase() to ErDoed,
    "forsvunnet".uppercase() to ErSavnet,
    "opphoert".uppercase() to OpphoertIdentitet,
    "dNummer".uppercase() to Dnummer
)

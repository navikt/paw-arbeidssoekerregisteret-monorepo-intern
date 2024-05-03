package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus

fun forenkletFregFakta(status: List<Folkeregisterpersonstatus>): Set<Opplysning> =
    status.map { it.forenkletStatus }
        .map { it.uppercase() }
        .map { enkelFolkeRegStatusTilOpplysning[it] ?: Opplysning.UKJENT_FORENKLET_FREG_STATUS }
        .toSet()

private val enkelFolkeRegStatusTilOpplysning: Map<String, Opplysning> = mapOf(
    "bosattEtterFolkeregisterloven".uppercase() to Opplysning.BOSATT_ETTER_FREG_LOVEN,
    "ikkeBosatt".uppercase() to Opplysning.IKKE_BOSATT,
    "doedIFolkeregisteret".uppercase() to Opplysning.DOED,
    "forsvunnet".uppercase() to Opplysning.SAVNET,
    "opphoert".uppercase() to Opplysning.OPPHOERT_IDENTITET,
    "dNummer".uppercase() to Opplysning.DNUMMER
)

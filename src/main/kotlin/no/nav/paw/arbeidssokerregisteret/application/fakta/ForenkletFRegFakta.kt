package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus

fun forenkletFregFakta(status: List<Folkeregisterpersonstatus>): Set<Opplysning> =
    status.map { it.forenkletStatus }
        .map { enkelFolkeRegStatusTilOpplysning[it] ?: Opplysning.UKJENT_FORENKLET_FREG_STATUS }
        .toSet()

private val enkelFolkeRegStatusTilOpplysning: Map<String, Opplysning> = mapOf(
    "bosattEtterFolkeregisterloven" to Opplysning.BOSATT_ETTER_FREG_LOVEN,
    "ikkeBosatt" to Opplysning.IKKE_BOSATT,
    "doedIFolkeregisteret" to Opplysning.DOED,
    "forsvunnet" to Opplysning.SAVNET,
    "opphoert" to Opplysning.OPPHOERT_IDENTITET,
    "dnummer" to Opplysning.DNUMMER
)

package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus

fun evalForenkletFRegStatus(status: List<Folkeregisterpersonstatus>): Set<Fakta> =
    status.map { it.forenkletStatus }
        .map { enkelFolkeRegStatusTilFakta[it] ?: Fakta.UKJENT_FORENKLET_FREG_STATUS }
        .toSet()

private val enkelFolkeRegStatusTilFakta: Map<String, Fakta> = mapOf(
    "bosattEtterFolkeregisterloven" to Fakta.BOSATT_ETTER_FREG_LOVEN,
    "ikkeBosatt" to Fakta.IKKE_BOSATT,
    "doedIFolkeregisteret" to Fakta.DOED,
    "forsvunnet" to Fakta.SAVNET,
    "opphoert" to Fakta.OPPHOERT_IDENTITET,
    "dnummer" to Fakta.DNUMMER
)

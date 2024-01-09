package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus

fun evalForenkletFRegStatus(status: List<Folkeregisterpersonstatus>): Set<Attributter> =
    status.map { it.forenkletStatus }
        .map { simpleStatusToAttributter[it] ?: Attributter.UKJENT_FORENKLET_FREG_STATUS }
        .toSet()

val simpleStatusToAttributter: Map<String, Attributter> = mapOf(
    "bosattEtterFolkeregisterloven" to Attributter.BOSATT_ETTER_FREG_LOVEN,
    "ikkeBosatt" to Attributter.IKKE_BOSATT,
    "doedIFolkeregisteret" to Attributter.DOED,
    "forsvunnet" to Attributter.SAVNET,
    "opphoert" to Attributter.OPPHOERT_IDENTITET,
    "dnummer" to Attributter.DNUMMER
)

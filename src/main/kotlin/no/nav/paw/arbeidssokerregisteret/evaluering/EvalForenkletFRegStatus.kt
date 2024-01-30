package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus

fun evalForenkletFRegStatus(status: List<Folkeregisterpersonstatus>): Set<Attributt> =
    status.map { it.forenkletStatus }
        .map { simpleStatusToAttributt[it] ?: Attributt.UKJENT_FORENKLET_FREG_STATUS }
        .toSet()

val simpleStatusToAttributt: Map<String, Attributt> = mapOf(
    "bosattEtterFolkeregisterloven" to Attributt.BOSATT_ETTER_FREG_LOVEN,
    "ikkeBosatt" to Attributt.IKKE_BOSATT,
    "doedIFolkeregisteret" to Attributt.DOED,
    "forsvunnet" to Attributt.SAVNET,
    "opphoert" to Attributt.OPPHOERT_IDENTITET,
    "dnummer" to Attributt.DNUMMER
)

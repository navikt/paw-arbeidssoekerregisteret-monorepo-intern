package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus

fun evalForenkletFRegStatus(status: List<Folkeregisterpersonstatus>): Set<Evaluation> =
    status.map { it.forenkletStatus }
        .map { simpleStatusToEvaluation[it] ?: Evaluation.UKJENT_FORENKLET_FREG_STATUS }
        .toSet()

val simpleStatusToEvaluation: Map<String, Evaluation> = mapOf(
    "bosattEtterFolkeregisterloven" to Evaluation.BOSATT_ETTER_FREG_LOVEN,
    "ikkeBosatt" to Evaluation.IKKE_BOSATT,
    "doedIFolkeregisteret" to Evaluation.DOED,
    "forsvunnet" to Evaluation.SAVNET,
    "opphoert" to Evaluation.OPPHOERT_IDENTITET,
    "dnummer" to Evaluation.DNUMMER
)

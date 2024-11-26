package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.vo.Data
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Type

fun hentData(
    hentAlias: (List<String>) -> List<LokaleAlias>,
    aktor: Aktor,
): Data =
    aktor.identifikatorer
        .map { it.idnummer }
        .let(hentAlias)
        .let { Data(aktor, it) }
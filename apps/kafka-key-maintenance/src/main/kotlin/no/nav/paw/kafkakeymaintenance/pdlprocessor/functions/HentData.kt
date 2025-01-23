package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Ident
import no.nav.paw.kafkakeymaintenance.vo.Data

fun hentData(
    hentAlias: (List<String>) -> List<LokaleAlias>,
    pdlIdentiteter: List<Ident>,
): Data =
    pdlIdentiteter
        .map { it.ident }
        .let(hentAlias)
        .let { Data(pdlIdentiteter, it) }
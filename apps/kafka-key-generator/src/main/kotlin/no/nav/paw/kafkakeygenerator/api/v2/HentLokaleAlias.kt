package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.*
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer

fun Applikasjon.hentLokaleAlias(antallPartisjoner: Int, identiteter: List<String>): Either<Failure, List<LokaleAlias>> {
    return identiteter.mapNotNull { identitet ->
        hentLokaleAlias(antallPartisjoner, Identitetsnummer(identitet))
            .recover(FailureCode.DB_NOT_FOUND) { right(null) }
    }.flatten()
        .map(List<LokaleAlias?>::filterNotNull) }
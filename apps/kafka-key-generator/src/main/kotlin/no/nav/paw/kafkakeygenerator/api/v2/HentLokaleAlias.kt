package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.*
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.flatten
import no.nav.paw.kafkakeygenerator.vo.recover
import no.nav.paw.kafkakeygenerator.vo.right

fun KafkaKeysService.hentLokaleAlias(antallPartisjoner: Int, identiteter: List<String>): Either<Failure, List<LokaleAlias>> {
    return identiteter.mapNotNull { identitet ->
        hentLokaleAlias(antallPartisjoner, Identitetsnummer(identitet))
            .recover(FailureCode.DB_NOT_FOUND) { right(null) }
    }.flatten()
        .map(List<LokaleAlias?>::filterNotNull) }
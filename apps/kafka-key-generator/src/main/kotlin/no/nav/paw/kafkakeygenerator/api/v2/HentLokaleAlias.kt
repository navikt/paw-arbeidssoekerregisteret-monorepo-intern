package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.model.Either
import no.nav.paw.kafkakeygenerator.model.Failure
import no.nav.paw.kafkakeygenerator.model.FailureCode
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.model.flatten
import no.nav.paw.kafkakeygenerator.model.recover
import no.nav.paw.kafkakeygenerator.model.right

fun KafkaKeysService.hentLokaleAlias(
    antallPartisjoner: Int,
    identiteter: List<String>
): Either<Failure, List<LokaleAlias>> {
    return identiteter.mapNotNull { identitet ->
        hentLokaleAlias(antallPartisjoner, Identitetsnummer(identitet))
            .recover(FailureCode.DB_NOT_FOUND) { right(null) }
    }.flatten()
        .map(List<LokaleAlias?>::filterNotNull)
}
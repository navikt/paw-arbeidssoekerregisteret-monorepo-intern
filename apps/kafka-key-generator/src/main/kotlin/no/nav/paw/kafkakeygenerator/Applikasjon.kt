package no.nav.paw.kafkakeygenerator

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.FailureCode.CONFLICT
import no.nav.paw.kafkakeygenerator.FailureCode.DB_NOT_FOUND
import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer

class Applikasjon(
    private val kafkaKeys: KafkaKeys,
    private val identitetsTjeneste: PdlIdentitesTjeneste
) {

    @WithSpan
    suspend fun hent(callId: CallId, identitet: Identitetsnummer): Either<Failure, Long> {
        return kafkaKeys.hent(identitet)
            .recover(DB_NOT_FOUND) {
                sjekkMotAliaser(callId, identitet)
            }
    }

    @WithSpan
    suspend fun hentEllerOpprett(callId: CallId, identitet: Identitetsnummer): Either<Failure, Long> {
        return hent(callId, identitet)
            .recover(DB_NOT_FOUND) {
                kafkaKeys.opprett(identitet)
            }.recover(CONFLICT) {
                kafkaKeys.hent(identitet)
            }
    }

    @WithSpan
    private suspend fun sjekkMotAliaser(callId: CallId, identitet: Identitetsnummer): Either<Failure, Long> {
        return identitetsTjeneste.hentIdentiter(callId, identitet)
            .flatMap(kafkaKeys::hent)
            .flatMap { ids ->
                ids.values
                    .firstOrNull()?.let(::right)
                    ?: left(Failure("database", DB_NOT_FOUND))
            }
            .onRight { key -> kafkaKeys.lagre(identitet, key) }
    }
}

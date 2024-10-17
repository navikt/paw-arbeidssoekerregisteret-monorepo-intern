package no.nav.paw.kafkakeygenerator

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.FailureCode.CONFLICT
import no.nav.paw.kafkakeygenerator.FailureCode.DB_NOT_FOUND
import no.nav.paw.kafkakeygenerator.api.v2.*
import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import kotlin.math.absoluteValue

class Applikasjon(
    private val kafkaKeys: KafkaKeys,
    private val identitetsTjeneste: PdlIdentitesTjeneste
) {

    @WithSpan
    suspend fun hentInfo(callId: CallId, identitet: Identitetsnummer): Either<Failure, InfoResponse> {
        val pdlIdInfo = identitetsTjeneste.hentIdentInformasjon(callId, identitet)
        return kafkaKeys.hent(identitet)
            .map { arbeidssoekerId ->
                LokalIdData(
                    arbeidsoekerId = arbeidssoekerId.value,
                    recordKey = publicTopicKeyFunction(arbeidssoekerId).value
                )
            }.map { lokalIdData ->
                InfoResponse(
                    lagretData = lokalIdData,
                    pdlData = pdlIdInfo.fold(
                        { PdlData(error = it.code.name, id = null) },
                        { PdlData(error = null, id = it.map { identInfo ->  PdlId(identInfo.gruppe.name, identInfo.ident) })}
                    )
                )
            }
    }

    @WithSpan
    suspend fun hent(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        return kafkaKeys.hent(identitet)
            .recover(DB_NOT_FOUND) {
                sjekkMotAliaser(callId, identitet)
            }
    }

    @WithSpan
    suspend fun hentEllerOpprett(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        return hent(callId, identitet)
            .recover(DB_NOT_FOUND) {
                kafkaKeys.opprett(identitet)
            }.recover(CONFLICT) {
                kafkaKeys.hent(identitet)
            }
    }

    @WithSpan
    private suspend fun sjekkMotAliaser(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
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

package no.nav.paw.kafkakeygenerator

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.FailureCode.CONFLICT
import no.nav.paw.kafkakeygenerator.FailureCode.DB_NOT_FOUND
import no.nav.paw.kafkakeygenerator.api.v2.*
import no.nav.paw.kafkakeygenerator.mergedetector.findMerge
import no.nav.paw.kafkakeygenerator.mergedetector.hentLagretData
import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetected
import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.vo.*
import org.apache.kafka.clients.producer.internals.BuiltInPartitioner.partitionForKey
import org.apache.kafka.common.serialization.Serdes

class Applikasjon(
    private val kafkaKeys: KafkaKeys,
    private val identitetsTjeneste: PdlIdentitesTjeneste
) {
    private val keySerializer = Serdes.Long().serializer()

    @WithSpan
    fun hentLokaleAlias(
        antallPartisjoner: Int,
        identitet: Identitetsnummer
    ): Either<Failure, LokaleAlias> {
        return kafkaKeys.hent(identitet)
            .map { arbeidssoekerId ->
                val recordKey = publicTopicKeyFunction(arbeidssoekerId)
                Alias(
                    identitetsnummer = identitet.value,
                    arbeidsoekerId = arbeidssoekerId.value,
                    recordKey = recordKey.value,
                    partition = partitionForKey(keySerializer.serialize("", recordKey.value), antallPartisjoner)
                )
            }.flatMap { alias ->
                kafkaKeys.hent(ArbeidssoekerId(alias.arbeidsoekerId))
                    .map { identiteter ->
                        identiteter.map { identitetsnummer ->
                            Alias(
                                identitetsnummer = identitetsnummer.value,
                                arbeidsoekerId = alias.arbeidsoekerId,
                                recordKey = alias.recordKey,
                                partition = alias.partition
                            )
                        }
                    }
            }.map { aliases ->
                LokaleAlias(
                    identitetsnummer = identitet.value,
                    koblinger = aliases
                )
            }
    }

    @WithSpan
    suspend fun validerLagretData(callId: CallId, identitet: Identitetsnummer): Either<Failure, InfoResponse> {
        return hentInfo(callId, identitet)
            .flatMap { info ->
                hentLagretData(
                    hentArbeidssoekerId = kafkaKeys::hent,
                    info = info
                ).map { info to it }
            }
            .map { (info, lagretDatra) -> info to findMerge(lagretDatra) }
            .map { (info, merge) ->
                InfoResponse(
                    info = info,
                    mergeDetected = merge as? MergeDetected
                )
            }
    }

    @WithSpan
    suspend fun hentInfo(callId: CallId, identitet: Identitetsnummer): Either<Failure, Info> {
        val pdlIdInfo = identitetsTjeneste.hentIdentInformasjon(
            callId = callId,
            identitet = identitet,
            histrorikk = true
        )
        return kafkaKeys.hent(identitet)
            .map { arbeidssoekerId ->
                LokalIdData(
                    arbeidsoekerId = arbeidssoekerId.value,
                    recordKey = publicTopicKeyFunction(arbeidssoekerId).value
                )
            }.map { lokalIdData ->
                Info(
                    identitetsnummer = identitet.value,
                    lagretData = lokalIdData,
                    pdlData = pdlIdInfo.fold(
                        { PdlData(error = it.code.name, id = null) },
                        {
                            PdlData(
                                error = null,
                                id = it.map { identInfo ->
                                    PdlId(
                                        gruppe = identInfo.gruppe.name,
                                        id = identInfo.ident,
                                        gjeldende = !identInfo.historisk
                                    )
                                })
                        }
                    )
                )
            }
    }

    @WithSpan
    suspend fun hent(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        return kafkaKeys.hent(identitet)
            .suspendingRecover(DB_NOT_FOUND) {
                sjekkMotAliaser(callId, identitet)
            }
    }

    @WithSpan
    suspend fun hentEllerOpprett(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        return hent(callId, identitet)
            .suspendingRecover(DB_NOT_FOUND) {
                kafkaKeys.opprett(identitet)
            }.recover(CONFLICT) {
                kafkaKeys.hent(identitet)
            }
    }

    @WithSpan
    private suspend fun sjekkMotAliaser(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        return identitetsTjeneste.hentIdentiter(
            callId = callId,
            identitet = identitet,
            histrorikk = true
        )
            .flatMap(kafkaKeys::hent)
            .flatMap { ids ->
                ids.values
                    .firstOrNull()?.let(::right)
                    ?: left(Failure("database", DB_NOT_FOUND))
            }
            .onRight { key -> kafkaKeys.lagre(identitet, key) }
    }
}

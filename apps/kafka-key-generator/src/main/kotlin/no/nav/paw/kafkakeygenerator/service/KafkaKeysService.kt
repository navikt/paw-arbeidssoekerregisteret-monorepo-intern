package no.nav.paw.kafkakeygenerator.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.kafkakeygenerator.api.v2.Alias
import no.nav.paw.kafkakeygenerator.api.v2.InfoResponse
import no.nav.paw.kafkakeygenerator.api.v2.LokaleAlias
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction
import no.nav.paw.kafkakeygenerator.mergedetector.findMerge
import no.nav.paw.kafkakeygenerator.mergedetector.hentLagretData
import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetected
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.utils.countRestApiFailed
import no.nav.paw.kafkakeygenerator.utils.countRestApiFetched
import no.nav.paw.kafkakeygenerator.utils.countRestApiInserted
import no.nav.paw.kafkakeygenerator.utils.countRestApiReceived
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode.CONFLICT
import no.nav.paw.kafkakeygenerator.vo.FailureCode.DB_NOT_FOUND
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.Info
import no.nav.paw.kafkakeygenerator.vo.LokalIdData
import no.nav.paw.kafkakeygenerator.vo.PdlData
import no.nav.paw.kafkakeygenerator.vo.PdlId
import no.nav.paw.kafkakeygenerator.vo.flatMap
import no.nav.paw.kafkakeygenerator.vo.left
import no.nav.paw.kafkakeygenerator.vo.recover
import no.nav.paw.kafkakeygenerator.vo.right
import no.nav.paw.kafkakeygenerator.vo.suspendingRecover
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.producer.internals.BuiltInPartitioner.partitionForKey
import org.apache.kafka.common.serialization.Serdes

class KafkaKeysService(
    private val meterRegistry: MeterRegistry,
    private val kafkaKeysRepository: KafkaKeysRepository,
    private val pdlService: PdlService
) {
    private val logger = buildLogger
    private val keySerializer = Serdes.Long().serializer()

    @WithSpan
    fun hentLokaleAlias(
        antallPartisjoner: Int,
        identitet: Identitetsnummer
    ): Either<Failure, LokaleAlias> {
        return kafkaKeysRepository.hent(identitet)
            .map { arbeidssoekerId ->
                val recordKey = publicTopicKeyFunction(arbeidssoekerId)
                Alias(
                    identitetsnummer = identitet.value,
                    arbeidsoekerId = arbeidssoekerId.value,
                    recordKey = recordKey.value,
                    partition = partitionForKey(keySerializer.serialize("", recordKey.value), antallPartisjoner)
                )
            }.flatMap { alias ->
                kafkaKeysRepository.hent(ArbeidssoekerId(alias.arbeidsoekerId))
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
                    hentArbeidssoekerId = kafkaKeysRepository::hent,
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
        val pdlIdInfo = pdlService.hentIdentInformasjon(
            callId = callId,
            identitet = identitet,
            histrorikk = true
        )
        return kafkaKeysRepository.hent(identitet)
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
        logger.debug("Henter identer fra database")
        meterRegistry.countRestApiFetched()
        return kafkaKeysRepository.hent(identitet)
            .suspendingRecover(DB_NOT_FOUND) {
                sjekkMotAliaser(callId, identitet)
            }
    }

    @WithSpan
    suspend fun hentEllerOpprett(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        meterRegistry.countRestApiReceived()
        return hent(callId, identitet)
            .suspendingRecover(DB_NOT_FOUND) {
                logger.debug("Oppretter identer i database")
                meterRegistry.countRestApiInserted()
                kafkaKeysRepository.opprett(identitet)
            }.recover(CONFLICT) {
                meterRegistry.countRestApiFailed()
                kafkaKeysRepository.hent(identitet)
            }
    }

    @WithSpan
    private suspend fun sjekkMotAliaser(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        logger.debug("Sjekker identer mot PDL")
        return pdlService.hentIdentiter(
            callId = callId,
            identitet = identitet,
            histrorikk = true
        )
            .flatMap(kafkaKeysRepository::hent)
            .flatMap { ids ->
                ids.values
                    .firstOrNull()?.let(::right)
                    ?: left(Failure("database", DB_NOT_FOUND))
            }
            .onRight { key -> kafkaKeysRepository.lagre(identitet, key) }
    }
}

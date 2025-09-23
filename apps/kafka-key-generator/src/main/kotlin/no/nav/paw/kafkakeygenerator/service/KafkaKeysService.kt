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
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.CallId
import no.nav.paw.kafkakeygenerator.model.Either
import no.nav.paw.kafkakeygenerator.model.Failure
import no.nav.paw.kafkakeygenerator.model.FailureCode
import no.nav.paw.kafkakeygenerator.model.FailureCode.CONFLICT
import no.nav.paw.kafkakeygenerator.model.FailureCode.DB_NOT_FOUND
import no.nav.paw.kafkakeygenerator.model.IdentitetFailure
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.model.Info
import no.nav.paw.kafkakeygenerator.model.LokalIdData
import no.nav.paw.kafkakeygenerator.model.PdlData
import no.nav.paw.kafkakeygenerator.model.PdlId
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.model.flatMap
import no.nav.paw.kafkakeygenerator.model.flatten
import no.nav.paw.kafkakeygenerator.model.left
import no.nav.paw.kafkakeygenerator.model.recover
import no.nav.paw.kafkakeygenerator.model.right
import no.nav.paw.kafkakeygenerator.model.suspendingRecover
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.utils.countRestApiFailed
import no.nav.paw.kafkakeygenerator.utils.countRestApiFetched
import no.nav.paw.kafkakeygenerator.utils.countRestApiInserted
import no.nav.paw.kafkakeygenerator.utils.countRestApiReceived
import no.nav.paw.logging.logger.buildLogger
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import org.apache.kafka.clients.producer.internals.BuiltInPartitioner.partitionForKey
import org.apache.kafka.common.serialization.Serdes

class KafkaKeysService(
    private val meterRegistry: MeterRegistry,
    private val kafkaKeysRepository: KafkaKeysRepository,
    private val pdlService: PdlService,
    private val identitetService: IdentitetService
) {
    private val logger = buildLogger
    private val keySerializer = Serdes.Long().serializer()

    fun hentLokaleAlias(
        antallPartisjoner: Int,
        identiteter: List<String>
    ): Either<Failure, List<LokaleAlias>> {
        return identiteter.mapNotNull { identitet ->
            hentLokaleAlias(antallPartisjoner, Identitetsnummer(identitet))
                .recover(FailureCode.DB_NOT_FOUND) { right(null) }
        }.flatten()
            .map(List<LokaleAlias?>::filterNotNull)
    }

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
                    .recover(DB_NOT_FOUND) { right(info to null) }
            }
            .map { (info, lagretData) -> info to lagretData?.let { findMerge(it) } }
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
            historikk = true
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
                        { PdlData(error = it.code().name, id = null) },
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
            }.recover(DB_NOT_FOUND) {
                right(
                    Info(
                    identitetsnummer = identitet.value,
                    lagretData = null,
                    pdlData = pdlIdInfo.fold(
                        { PdlData(error = it.code().name, id = null) },
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
                    )))
            }
    }

    @WithSpan
    fun kunHent(identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        logger.debug("Henter identer fra database")
        meterRegistry.countRestApiFetched()
        return kafkaKeysRepository.hent(identitet)
    }

    @WithSpan
    suspend fun hentEllerOppdater(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        logger.debug("Henter identer fra database")
        meterRegistry.countRestApiFetched()
        return kafkaKeysRepository.hent(identitet)
            .suspendingRecover(DB_NOT_FOUND) {
                sjekkMotAliaser(callId, identitet)
                    .flatMap { (arbeidssoekerId, identInformasjon) ->
                        kafkaKeysRepository.lagre(identitet, arbeidssoekerId)
                        val identiteter = identInformasjon.map { it.asIdentitet() }
                        identitetService.identiteterSkalOppdateres(identiteter)
                        right(arbeidssoekerId)
                    }
            }
    }

    @WithSpan
    suspend fun hentEllerOpprett(callId: CallId, identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        meterRegistry.countRestApiReceived()
        return hentEllerOppdater(callId, identitet)
            .suspendingRecover(DB_NOT_FOUND) { failure ->
                logger.debug("Oppretter identer i database")
                meterRegistry.countRestApiInserted()
                val result = kafkaKeysRepository.opprett(identitet)
                if (failure is IdentitetFailure) {
                    val identiteter = failure.identInformasjon().map { it.asIdentitet() }
                    identitetService.identiteterSkalOppdateres(identiteter)
                }
                result
            }.recover(CONFLICT) {
                meterRegistry.countRestApiFailed()
                kafkaKeysRepository.hent(identitet)
            }
    }

    @WithSpan
    private suspend fun sjekkMotAliaser(
        callId: CallId,
        identitet: Identitetsnummer
    ): Either<Failure, Pair<ArbeidssoekerId, List<IdentInformasjon>>> {
        logger.debug("Sjekker identer mot PDL")
        return pdlService.hentIdentInformasjon(
            callId = callId,
            identitet = identitet,
            historikk = true
        ).flatMap { identiteter ->
            kafkaKeysRepository.hent(identiteter.map { it.ident })
                .map { it to identiteter }
        }.flatMap { (identitetMap, identInformasjon) ->
            if (identitetMap.isNotEmpty()) {
                val arbeidssoekerId = identitetMap.values.first()
                right(arbeidssoekerId to identInformasjon)
            } else {
                left(
                    IdentitetFailure(
                        system = "database",
                        code = DB_NOT_FOUND,
                        identInformasjon = identInformasjon
                    )
                )
            }
        }
    }
}

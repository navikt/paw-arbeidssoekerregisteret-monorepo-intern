package no.nav.paw.kafkakeygenerator.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.api.v2.Alias
import no.nav.paw.kafkakeygenerator.api.v2.InfoResponse
import no.nav.paw.kafkakeygenerator.api.v2.LokaleAlias
import no.nav.paw.kafkakeygenerator.exception.IdentitetIkkeFunnetException
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.CallId
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.model.Info
import no.nav.paw.kafkakeygenerator.model.LokalIdData
import no.nav.paw.kafkakeygenerator.model.MergeDetected
import no.nav.paw.kafkakeygenerator.model.PdlData
import no.nav.paw.kafkakeygenerator.model.PdlId
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.kafkakeygenerator.utils.asRecordKey
import no.nav.paw.kafkakeygenerator.utils.countRestApiFetched
import no.nav.paw.kafkakeygenerator.utils.countRestApiReceived
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.producer.internals.BuiltInPartitioner.partitionForKey
import org.apache.kafka.common.serialization.Serdes

class KafkaKeysService(
    private val meterRegistry: MeterRegistry,
    private val pdlService: PdlService,
    private val identitetRepository: IdentitetRepository,
    private val identitetService: IdentitetService
) {
    private val logger = buildLogger
    private val keySerializer = Serdes.Long().serializer()

    @WithSpan
    fun hentEllerOppdater(
        callId: CallId,
        identitet: Identitetsnummer
    ): ArbeidssoekerId {
        meterRegistry.countRestApiFetched()
        logger.debug("Henter eller oppdaterer arbeidssoekerId for identitet")
        return hentEllerLagre(
            callId = callId,
            identitet = identitet,
            lagreIdentiteter = identitetService::identiteterSkalOppdateres
        )
    }

    @WithSpan
    fun hentEllerOpprett(
        callId: CallId,
        identitet: Identitetsnummer
    ): ArbeidssoekerId {
        meterRegistry.countRestApiReceived()
        logger.debug("Henter eller oppretter arbeidssoekerId for identitet")
        return hentEllerLagre(
            callId = callId,
            identitet = identitet,
            lagreIdentiteter = identitetService::identiteterSkalOpprettes
        )
    }

    private fun hentEllerLagre(
        callId: CallId,
        identitet: Identitetsnummer,
        lagreIdentiteter: (identiteter: List<Identitet>) -> Unit
    ): ArbeidssoekerId {
        val identitetRows = identitetRepository.findAllByIdentitet(identitet.value)
            .filter { it.status != IdentitetStatus.SLETTET }
        val arbeidssoekerId = identitetRows
            .find { it.identitet == identitet.value }
            ?.let { ArbeidssoekerId(it.arbeidssoekerId) }
            ?: identitetRows
                .map { ArbeidssoekerId(it.arbeidssoekerId) }
                .maxByOrNull { it.value }
        return if (arbeidssoekerId != null) {
            arbeidssoekerId
        } else {
            logger.debug("Ingen arbeidssoekerId funnet for identitet")
            val identiteter = pdlService.finnIdentiteter(
                callId = callId,
                identitet = identitet.value,
                historikk = true
            ).map { it.asIdentitet() }
            lagreIdentiteter(identiteter)
            val lagredeIdenitetRows = identitetRepository.findAllByIdentitet(identitet.value)
                .filter { it.status != IdentitetStatus.SLETTET }
            if (lagredeIdenitetRows.isEmpty()) {
                throw IdentitetIkkeFunnetException()
            } else {
                val lagredeIdenitetRow = lagredeIdenitetRows.find { it.identitet == identitet.value }
                if (lagredeIdenitetRow != null) {
                    ArbeidssoekerId(lagredeIdenitetRow.arbeidssoekerId)
                } else {
                    ArbeidssoekerId(lagredeIdenitetRows.maxOf { it.arbeidssoekerId })
                }
            }
        }
    }

    @WithSpan
    fun hentLokaleAlias(
        antallPartisjoner: Int,
        identiteter: List<String>
    ): List<LokaleAlias> {
        return identiteter
            .map { hentLokaleAlias(antallPartisjoner, it) }
    }

    @WithSpan
    fun hentLokaleAlias(
        antallPartisjoner: Int,
        identitet: String
    ): LokaleAlias {
        val aliases = identitetRepository.findAllByIdentitet(identitet)
            .filter { it.status != IdentitetStatus.SLETTET }
            .map {
                val recordKey = it.arbeidssoekerId.asRecordKey()
                val partition = partitionForKey(keySerializer.serialize("", recordKey), antallPartisjoner)
                Alias(
                    identitetsnummer = it.identitet,
                    arbeidsoekerId = it.arbeidssoekerId,
                    recordKey = recordKey,
                    partition = partition
                )
            }
        return LokaleAlias(
            identitetsnummer = identitet,
            koblinger = aliases
        )
    }

    @WithSpan
    fun hentInfo(
        callId: CallId,
        identitet: Identitetsnummer
    ): InfoResponse {
        val pdlIdentiteter = pdlService.finnIdentiteter(
            callId = callId,
            identitet = identitet.value,
            historikk = true
        )
        val pdlData = PdlData(
            error = null,
            id = pdlIdentiteter.map {
                PdlId(
                    gruppe = it.gruppe.name,
                    id = it.ident,
                    gjeldende = !it.historisk
                )
            })
        val alleIdentiteter = identitetRepository.findAllByIdentitet(identitet.value)
            .filter { it.status != IdentitetStatus.SLETTET }

        val lokalIdData = if (alleIdentiteter.isEmpty()) {
            null
        } else {
            val arbeidssoekerId = alleIdentiteter
                .find { it.identitet == identitet.value }?.arbeidssoekerId
                ?: alleIdentiteter.maxOf { it.arbeidssoekerId }
            LokalIdData(
                arbeidsoekerId = arbeidssoekerId,
                recordKey = arbeidssoekerId.asRecordKey()
            )
        }

        val info = Info(
            identitetsnummer = identitet.value,
            lagretData = lokalIdData,
            pdlData = pdlData
        )

        val etterArbeidssoekerId = alleIdentiteter
            .groupBy { it.arbeidssoekerId }
            .mapKeys { ArbeidssoekerId(it.key) }
            .mapValues { it.value.map { identitet -> Identitetsnummer(identitet.identitet) } }
        val merge = if (etterArbeidssoekerId.size > 1) {
            MergeDetected(identitet, etterArbeidssoekerId)
        } else {
            null
        }

        return InfoResponse(
            info = info,
            mergeDetected = merge
        )
    }
}

package no.nav.paw.arbeidssoekerregisteret.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.models.BestillingResponse
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.exception.BestillingIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.exception.VarselIkkeFunnetException
import no.nav.paw.arbeidssoekerregisteret.model.BestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.BestillingStatus
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.InsertBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.UpdateBestillingRow
import no.nav.paw.arbeidssoekerregisteret.model.UpdateBestiltVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.model.asInsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asResponse
import no.nav.paw.arbeidssoekerregisteret.repository.BestillingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.BestiltVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.arbeidssoekerregisteret.utils.Source
import no.nav.paw.arbeidssoekerregisteret.utils.beskjedVarselCounter
import no.nav.paw.arbeidssoekerregisteret.utils.sendVarsel
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.clients.producer.Producer
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class BestillingService(
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: MeterRegistry,
    private val bestillingRepository: BestillingRepository,
    private val bestiltVarselRepository: BestiltVarselRepository,
    private val varselRepository: VarselRepository,
    private val varselKafkaProducer: Producer<String, String>,
    private val varselMeldingBygger: VarselMeldingBygger
) {
    private val logger = buildApplicationLogger

    @WithSpan("hentBestilling")
    fun hentBestilling(bestillingId: UUID): BestillingResponse = transaction {
        val bestilling = bestillingRepository.findByBestillingId(bestillingId)
            ?: throw BestillingIkkeFunnetException("Bestilling ikke funnet")
        val totalCount = bestiltVarselRepository
            .countByBestillingId(bestillingId)
        val sendtCount = bestiltVarselRepository
            .countByBestillingIdAndStatus(bestillingId, BestiltVarselStatus.SENDT)
        val feiletCount = bestiltVarselRepository
            .countByBestillingIdAndStatus(bestillingId, BestiltVarselStatus.FEILET)
        val ignorertCount = bestiltVarselRepository
            .countByBestillingIdAndStatus(bestillingId, BestiltVarselStatus.IGNORERT)
        bestilling.asResponse(totalCount, sendtCount, feiletCount, ignorertCount)
    }

    @WithSpan("opprettBestilling")
    fun opprettBestilling(bestiller: String): BestillingResponse = transaction {
        val bestillingId = UUID.randomUUID()
        bestillingRepository.insert(InsertBestillingRow(bestillingId, bestiller))
        bestillingRepository.findByBestillingId(bestillingId)?.asResponse(0, 0, 0, 0)
            ?: throw BestillingIkkeFunnetException("Bestilling ikke funnet")
    }

    @WithSpan("bekreftBestilling")
    fun bekreftBestilling(bestillingId: UUID): BestillingResponse = transaction {
        val bestilling = bestillingRepository.findByBestillingId(bestillingId)
            ?: throw BestillingIkkeFunnetException("Bestilling ikke funnet")
        if (bestilling.status == BestillingStatus.VENTER) {
            bestiltVarselRepository.insertAktivePerioder(bestillingId)
            bestillingRepository.update(UpdateBestillingRow(bestillingId, BestillingStatus.BEKREFTET))
        }
        hentBestilling(bestillingId)
    }

    @WithSpan("prosesserBestillinger")
    fun prosesserBestillinger() {
        val bestillinger = bestillingRepository.findByStatus(BestillingStatus.BEKREFTET)
        if (bestillinger.isEmpty()) {
            logger.info("Ingen ventende manuelle varselbestillinger funnet")
        } else {
            if (!applicationConfig.manuelleVarslerEnabled) {
                logger.warn("Utsendelse av manuelle varsler er deaktivert")
            }
            logger.info("Starter prosessering av {} manuelle varselbestillinger", bestillinger.size)
            bestillinger
                .map { UpdateBestillingRow(it.bestillingId, BestillingStatus.AKTIV) }
                .forEach { bestillingRepository.update(it) }
            bestillinger.forEach { prosesserBestilling(it) }
        }
    }

    @WithSpan("prosesserBestilling")
    private fun prosesserBestilling(bestilling: BestillingRow) {
        val varselIdList = bestiltVarselRepository
            .findVarselIdByBestillingIdAndStatus(bestilling.bestillingId, BestiltVarselStatus.VENTER)
        logger.info(
            "Prosesserer {} varslinger for varselbestilling {}",
            varselIdList.size,
            bestilling.bestillingId
        )
        val status = if (varselIdList.isEmpty()) {
            BestillingStatus.IGNORERT
        } else {
            val varselStatuser = varselIdList
                .map { prosesserBestiltVarsel(it) }
                .map { it.status }
            if (varselStatuser.all { it == BestiltVarselStatus.IGNORERT }) {
                BestillingStatus.IGNORERT
            } else {
                varselStatuser.filter { it != BestiltVarselStatus.SENDT }
                    .let { if (it.isEmpty()) BestillingStatus.SENDT else BestillingStatus.FEILET }
            }
        }
        logger.info(
            "Prosessering av varselbestilling {} fullførte med status {}",
            bestilling.bestillingId,
            status
        )
        bestillingRepository.update(UpdateBestillingRow(bestilling.bestillingId, status))
    }

    @WithSpan("prosesserBestiltVarsel")
    private fun prosesserBestiltVarsel(varselId: UUID): BestiltVarselRow = transaction {
        val varsel = bestiltVarselRepository.findByVarselId(varselId)
            ?: throw VarselIkkeFunnetException("Manuelt varsel ikke funnet")
        try {
            logger.info("Prosesserer manuelt varsel {}", varselId)
            if (applicationConfig.manuelleVarslerEnabled) {
                val insertVarselRow = varsel.asInsertVarselRow()
                varselRepository.insert(insertVarselRow)
                val melding = varselMeldingBygger.opprettManueltVarsel(varsel.varselId, varsel.identitetsnummer)
                meterRegistry.beskjedVarselCounter(Source.DATABASE, melding)
                varselKafkaProducer.sendVarsel(applicationConfig.tmsVarselTopic, melding)
                logger.debug("Sendte manuelt varsel {}", varsel.varselId)
                bestiltVarselRepository.update(UpdateBestiltVarselRow(varsel.varselId, BestiltVarselStatus.SENDT))
            } else {
                bestiltVarselRepository.update(
                    UpdateBestiltVarselRow(
                        varsel.varselId,
                        BestiltVarselStatus.IGNORERT
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Sending av manuelt varsel feilet", e)
            bestiltVarselRepository.update(UpdateBestiltVarselRow(varsel.varselId, BestiltVarselStatus.FEILET))
        }
        bestiltVarselRepository.findByVarselId(varsel.varselId)
            ?: throw VarselIkkeFunnetException("Manuelt varsel ikke funnet")
    }
}
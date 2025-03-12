package no.nav.paw.arbeidssoekerregisteret.service

import io.micrometer.core.instrument.MeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.models.BestillingResponse
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.config.ServerConfig
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
import no.nav.paw.arbeidssoekerregisteret.utils.sendVarsel
import no.nav.paw.arbeidssoekerregisteret.utils.varselCounter
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.clients.producer.Producer
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class BestillingService(
    private val serverConfig: ServerConfig,
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: MeterRegistry,
    private val bestillingRepository: BestillingRepository,
    private val bestiltVarselRepository: BestiltVarselRepository,
    private val varselRepository: VarselRepository,
    private val varselKafkaProducer: Producer<String, String>,
    private val varselMeldingBygger: VarselMeldingBygger
) {
    private val logger = buildApplicationLogger

    fun hentBestilling(bestillingId: UUID): BestillingResponse = transaction {
        val bestilling = bestillingRepository.findByBestillingId(bestillingId)
            ?: throw BestillingIkkeFunnetException("Bestilling ikke funnet")
        val totalCount = bestiltVarselRepository.countByBestillingId(bestillingId)
        val sendtCount = bestiltVarselRepository.countByBestillingIdAndStatus(bestillingId, BestiltVarselStatus.SENDT)
        val feiletCount = bestiltVarselRepository.countByBestillingIdAndStatus(bestillingId, BestiltVarselStatus.FEILET)
        bestilling.asResponse(totalCount, sendtCount, feiletCount)
    }

    fun opprettBestilling(bestiller: String): BestillingResponse = transaction {
        val bestillingId = UUID.randomUUID()
        bestillingRepository.insert(InsertBestillingRow(bestillingId, bestiller))
        bestillingRepository.findByBestillingId(bestillingId)?.asResponse(0, 0, 0)
            ?: throw BestillingIkkeFunnetException("Bestilling ikke funnet")
    }

    fun bekreftBestilling(bestillingId: UUID): BestillingResponse = transaction {
        val bestilling = bestillingRepository.findByBestillingId(bestillingId)
            ?: throw BestillingIkkeFunnetException("Bestilling ikke funnet")
        if (bestilling.status == BestillingStatus.VENTER) {
            bestiltVarselRepository.insertAktivePerioder(bestillingId)
            bestillingRepository.update(UpdateBestillingRow(bestillingId, BestillingStatus.BEKREFTET))
        }
        hentBestilling(bestillingId)
    }

    fun prosesserBestillinger() = transaction {
        val bestillinger = bestillingRepository.findByStatus(BestillingStatus.BEKREFTET)
        if (bestillinger.isEmpty()) {
            logger.info("Ingen ventende manuelle varselbestillinger funnet")
        } else {
            if (!applicationConfig.manuelleVarslerEnabled) {
                logger.warn("Utsendelse av manuelle varsler er deaktivert")
            }
            bestillinger
                .map { UpdateBestillingRow(it.bestillingId, BestillingStatus.AKTIV) }
                .forEach { bestillingRepository.update(it) }
            logger.info("Starter prosessering av {} manuelle varselbestillinger", bestillinger.size)
            bestillinger.forEach { prosesserBestilling(it) }
        }
    }

    private fun prosesserBestilling(bestilling: BestillingRow) {
        val varslinger = bestiltVarselRepository.findByBestillingId(bestilling.bestillingId)
        logger.info(
            "Prosesserer {} varslinger for varselbestilling {}",
            varslinger.size,
            bestilling.bestillingId
        )
        val status = if (varslinger.isEmpty()) {
            BestillingStatus.IGNORERT
        } else {
            val varselStatuser = varslinger.map { prosesserBestiltVarsel(it) }.map { it.status }
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

    private fun prosesserBestiltVarsel(varsel: BestiltVarselRow): BestiltVarselRow {
        try {
            logger.debug("Prosesserer manuelt varsel {}", varsel.varselId)
            if (applicationConfig.manuelleVarslerEnabled) {
                val insertVarselRow = varsel.asInsertVarselRow()
                varselRepository.insert(insertVarselRow)
                val melding = varselMeldingBygger.opprettManueltVarsel(varsel.varselId, varsel.identitetsnummer)
                meterRegistry.varselCounter(serverConfig.runtimeEnvironment, "write", melding)
                varselKafkaProducer.sendVarsel(applicationConfig.tmsVarselTopic, melding)
                logger.debug("Sendte manuelt varsel {}", varsel.varselId)
                bestiltVarselRepository.update(UpdateBestiltVarselRow(varsel.varselId, BestiltVarselStatus.SENDT))
            } else {
                bestiltVarselRepository.update(UpdateBestiltVarselRow(varsel.varselId, BestiltVarselStatus.IGNORERT))
            }
        } catch (e: Exception) {
            logger.debug("Sending av manuelt varsel feilet", e)
            bestiltVarselRepository.update(UpdateBestiltVarselRow(varsel.varselId, BestiltVarselStatus.FEILET))
        }
        return bestiltVarselRepository.findByVarselId(varsel.varselId)
            ?: throw VarselIkkeFunnetException("Manuelt varsel ikke funnet")
    }
}
package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.BestillingStatus
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.repository.BestillingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.BestiltVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.EksterntVarselRepository
import no.nav.paw.arbeidssoekerregisteret.repository.VarselRepository
import no.nav.paw.logging.logger.buildLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class OppryddingService(
    private val applicationConfig: ApplicationConfig,
    private val bestillingRepository: BestillingRepository,
    private val bestiltVarselRepository: BestiltVarselRepository,
    private val varselRepository: VarselRepository,
    private val eksterntVarselRepository: EksterntVarselRepository
) {
    private val logger = buildLogger

    fun prosesserOpprydding() {
        logger.info("Starter oppryddingsjobb")
        val slettEldreEnn = Instant.now().minus(applicationConfig.oppryddingEldreEnn)
        ryddBestillinger(slettEldreEnn)
        ryddVarsler(slettEldreEnn)
    }

    private fun ryddBestillinger(slettEldreEnn: Instant) = transaction {
        val bestillinger = bestillingRepository.findByUpdatedTimestampAndStatus(
            slettEldreEnn,
            BestillingStatus.SENDT,
            BestillingStatus.IGNORERT
        )

        if (bestillinger.isEmpty()) {
            logger.info("Fant ingen bestillinger endre enn {}", slettEldreEnn)
        } else {
            logger.info("Utfører opprydding av {} bestillinger eldre enn {}", bestillinger.size, slettEldreEnn)
            bestillinger.forEach { bestilling ->
                val bestillingId = bestilling.bestillingId
                val bestilteVarslerSlettet = bestiltVarselRepository.deleteByBestillingIdAndUpdatedTimestampAndStatus(
                    bestillingId,
                    slettEldreEnn,
                    BestiltVarselStatus.SENDT,
                    BestiltVarselStatus.IGNORERT
                )
                logger.info(
                    "Slettet {} bestilte varsler eldre enn {} for bestilling {}",
                    bestilteVarslerSlettet,
                    slettEldreEnn,
                    bestillingId
                )
                val bestilteVarsler = bestiltVarselRepository.findByBestillingId(bestillingId)
                if (bestilteVarsler.isEmpty()) {
                    val bestillingerSlettet = bestillingRepository.deleteByBestillingId(bestillingId)
                    if (bestillingerSlettet > 0) {
                        logger.info("Sletting av bestilling {} fullført", bestillingId)
                    } else {
                        logger.warn("Sletting av bestilling {} førte ikke til noen endringer i databasen", bestillingId)
                    }
                } else {
                    logger.warn("Bestilling {} kunne ikke slettes da den fortsatt har aktive varsler", bestillingId)
                }
            }
        }
    }

    private fun ryddVarsler(slettEldreEnn: Instant) = transaction {
        logger.info("Utfører opprydding av eksterne varsler eldre enn {}", slettEldreEnn)
        val eksterneVarslerSlettet = eksterntVarselRepository.deleteByUpdatedTimestampAndStatus(
            slettEldreEnn,
            VarselStatus.SENDT,
            VarselStatus.KANSELLERT,
            VarselStatus.FERDIGSTILT
        )
        if (eksterneVarslerSlettet > 0) {
            logger.info("Sletting {} av eksterne varsler fullført", eksterneVarslerSlettet)
        } else {
            logger.warn("Sletting av eksterne varsler førte ikke til noen endringer i databasen")
        }

        logger.info("Utfører opprydding av varsler eldre enn {}", slettEldreEnn)
        val varslerSlettet = varselRepository.deleteByUpdatedTimestampAndStatus(
            slettEldreEnn,
            VarselStatus.SENDT,
            VarselStatus.KANSELLERT,
            VarselStatus.FERDIGSTILT
        )
        if (varslerSlettet > 0) {
            logger.info("Sletting {} av varsler fullført", varslerSlettet)
        } else {
            logger.warn("Sletting av varsler førte ikke til noen endringer i databasen")
        }
    }
}
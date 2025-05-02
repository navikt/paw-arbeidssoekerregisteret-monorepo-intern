package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.kafkakeygenerator.model.IdentitetKonfliktStatus
import no.nav.paw.kafkakeygenerator.repository.IdentitetKonfliktRepository
import no.nav.paw.logging.logger.buildLogger

class IdentitetKonfliktService(
    private val identitetKonfliktRepository: IdentitetKonfliktRepository,
) {
    private val logger = buildLogger

    fun lagreVentendeIdentitetKonflikt(
        aktorId: String,
    ) {
        val rows = identitetKonfliktRepository.findByAktorId(aktorId)
        if (rows.isEmpty()) {
            val rowsAffected = identitetKonfliktRepository.insert(
                aktorId = aktorId,
                status = IdentitetKonfliktStatus.VENTER
            )
            logger.info(
                "Lagret identitet-konflikt med status {} (rows affected {})",
                IdentitetKonfliktStatus.VENTER.name,
                rowsAffected
            )
        } else {
            logger.info("Innskag for identitet-konflikt finnes allerede")
        }
    }
}
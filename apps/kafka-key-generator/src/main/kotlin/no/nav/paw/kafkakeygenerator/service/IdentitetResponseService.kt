package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.kafkakeygenerator.api.models.IdentitetResponse
import no.nav.paw.kafkakeygenerator.api.models.Konflikt
import no.nav.paw.kafkakeygenerator.api.models.KonfliktDetaljer
import no.nav.paw.kafkakeygenerator.api.models.KonfliktType
import no.nav.paw.kafkakeygenerator.api.v2.asApi
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.asApi
import no.nav.paw.kafkakeygenerator.model.dao.IdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KonflikterTable
import no.nav.paw.kafkakeygenerator.model.dto.asIdentitet
import no.nav.paw.kafkakeygenerator.utils.asRecordKey
import no.nav.paw.logging.logger.buildLogger

class IdentitetResponseService(
    private val pdlService: PdlService
) {
    private val logger = buildLogger

    fun finnForIdentitet(
        identitet: String,
        visKonflikter: Boolean = false,
        hentPdl: Boolean = false
    ): IdentitetResponse {
        logger.debug("Henter alle identiteter relatert til gitt identitet")
        val pdlIdentiteter = if (hentPdl) {
            pdlService.finnIdentiteter(identitet = identitet)
                .map { it.asIdentitet() }
                .map { it.asApi() }
        } else {
            null
        }
        val identitetRows = IdentiteterTable
            .findAllByIdentitet(identitet)
            .filter { it.status != IdentitetStatus.SLETTET }
        if (identitetRows.isEmpty()) {
            return IdentitetResponse(
                identiteter = emptyList(),
                pdlIdentiteter = pdlIdentiteter,
            )
        } else {
            val arbeidssoekerId = identitetRows.maxOf { it.arbeidssoekerId }
            val konflikter = if (visKonflikter) {
                val aktorIdListe = identitetRows
                    .map { it.aktorId }
                    .distinct()
                val arbeidssoekerIdListe = identitetRows
                    .map { it.arbeidssoekerId }
                    .distinct()
                val konflikter = KonflikterTable.findByIdentitetAndStatus(
                    identitet = identitet,
                    status = KonfliktStatus.VENTER
                )
                if (konflikter.isNotEmpty()) {
                    konflikter
                        .map {
                            Konflikt(
                                type = it.type.asApi(),
                                detaljer = KonfliktDetaljer(
                                    aktorIdListe = aktorIdListe,
                                    arbeidssoekerIdListe = arbeidssoekerIdListe
                                )
                            )
                        }
                } else if (aktorIdListe.size > 1 || arbeidssoekerIdListe.size > 1) {
                    listOf(
                        Konflikt(
                            type = KonfliktType.MERGE,
                            detaljer = KonfliktDetaljer(
                                aktorIdListe = aktorIdListe,
                                arbeidssoekerIdListe = arbeidssoekerIdListe
                            )
                        )
                    )
                } else {
                    emptyList()
                }
            } else {
                null
            }
            val identiteter = identitetRows
                .map { it.asIdentitet() }
                .toMutableList()
                .apply { add(arbeidssoekerId.asIdentitet()) }
                .sortedBy { it.type.ordinal }
                .map { it.asApi() }
            return IdentitetResponse(
                arbeidssoekerId = arbeidssoekerId,
                recordKey = arbeidssoekerId.asRecordKey(),
                identiteter = identiteter,
                pdlIdentiteter = pdlIdentiteter,
                konflikter = konflikter
            )
        }
    }
}
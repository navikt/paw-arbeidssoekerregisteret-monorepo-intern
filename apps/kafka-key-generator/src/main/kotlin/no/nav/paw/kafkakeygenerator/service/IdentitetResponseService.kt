package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.kafkakeygenerator.api.models.IdentitetResponse
import no.nav.paw.kafkakeygenerator.api.models.Konflikt
import no.nav.paw.kafkakeygenerator.api.models.KonfliktType
import no.nav.paw.kafkakeygenerator.api.models.MergeKonflikt
import no.nav.paw.kafkakeygenerator.api.v2.asApi
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.repository.IdentitetRepository
import no.nav.paw.logging.logger.buildLogger

class IdentitetResponseService(
    private val identitetRepository: IdentitetRepository,
    private val pdlService: PdlService
) {
    private val logger = buildLogger

    suspend fun finnForIdentitet(
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
        val identitetRows = identitetRepository
            .findByIdentitet(identitet)
            .filter { it.status != IdentitetStatus.SLETTET }
        if (identitetRows.isEmpty()) {
            return IdentitetResponse(
                identiteter = emptyList(),
                pdlIdentiteter = pdlIdentiteter,
            )
        } else {
            val aktorIdListe = identitetRows.map { it.aktorId }.distinct()
            val arbeidssoekerIdListe = identitetRows.map { it.arbeidssoekerId }.distinct()
            val arbeidssoekerId = identitetRows.maxOf { it.arbeidssoekerId }
            val konflikter = if (visKonflikter) {
                if (aktorIdListe.size > 1 || arbeidssoekerIdListe.size > 1) {
                    listOf(
                        Konflikt(
                            type = KonfliktType.MERGE,
                            detaljer = MergeKonflikt(
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
                .apply { add(arbeidssoekerId.asIdentitet(true)) }
                .sortedBy { it.type.ordinal }
                .map { it.asApi() }
            return IdentitetResponse(
                arbeidssoekerId = arbeidssoekerId,
                recordKey = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId)).value,
                identiteter = identiteter,
                pdlIdentiteter = pdlIdentiteter,
                konflikter = konflikter
            )
        }
    }
}
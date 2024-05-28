package no.nav.paw.rapportering.internehendelser

import java.util.*

interface RapporteringsHendelse {
    val hendelseType: String
    val hendelseId: UUID
    val periodeId: UUID
    val identitetsnummer: String

    /**
     * Unik id for personen, generert av kafka-key-generator
     */
    val arbeidssoekerId: Long
}


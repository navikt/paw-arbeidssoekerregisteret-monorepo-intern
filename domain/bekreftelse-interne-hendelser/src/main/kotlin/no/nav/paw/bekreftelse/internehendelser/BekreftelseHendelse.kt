package no.nav.paw.bekreftelse.internehendelser

import java.util.*

interface BekreftelseHendelse {
    val hendelseType: String
    val hendelseId: UUID
    val periodeId: UUID
    val identitetsnummer: String

    /**
     * Unik id for personen, generert av kafka-key-generator
     */
    val arbeidssoekerId: Long
}


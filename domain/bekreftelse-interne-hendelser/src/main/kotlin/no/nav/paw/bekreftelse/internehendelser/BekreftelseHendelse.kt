package no.nav.paw.bekreftelse.internehendelser

import java.time.Instant
import java.util.*

sealed interface BekreftelseHendelse {
    val hendelseTidspunkt: Instant
    val hendelseType: String
    val hendelseId: UUID
    val periodeId: UUID

    /**
     * Unik id for personen, generert av kafka-key-generator
     */
    val arbeidssoekerId: Long
}


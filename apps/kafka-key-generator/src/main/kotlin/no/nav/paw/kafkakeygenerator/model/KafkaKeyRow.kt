package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.kafkakeygenerator.database.IdentitetTabell
import org.jetbrains.exposed.sql.ResultRow

data class KafkaKeyRow(
    val arbeidssoekerId: Long,
    val identitetsnummer: String
)

fun ResultRow.asKafkaKeyRow(): KafkaKeyRow = KafkaKeyRow(
    arbeidssoekerId = this[IdentitetTabell.kafkaKey],
    identitetsnummer = this[IdentitetTabell.identitetsnummer]
)

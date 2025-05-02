package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.kafkakeygenerator.database.IdentitetTabell
import org.jetbrains.exposed.sql.ResultRow

data class AggregertIdentitetRow(
    val identitet: IdentitetRow?,
    val kafkaKey: KafkaKeyRow?
)

fun ResultRow.asAggregertIdentitetRow(): AggregertIdentitetRow {
    return AggregertIdentitetRow(
        identitet = getOrNull(IdentiteterTable.id)?.let { asIdentitetRow() },
        kafkaKey = getOrNull(IdentitetTabell.kafkaKey)?.let { asKafkaKeyRow() },
    )
}
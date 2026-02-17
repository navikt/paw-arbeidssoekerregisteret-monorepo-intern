package no.nav.paw.kafka.model

import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Instant

data class HwmRow(
    val version: Int,
    val topic: String,
    val partition: Int,
    val offset: Long,
    val timestamp: Instant,
    val updatedTimestamp: Instant
)

fun ResultRow.asHwmRow(): HwmRow = HwmRow(
    version = this[HwmTable.version],
    topic = this[HwmTable.topic],
    partition = this[HwmTable.partition],
    offset = this[HwmTable.offset],
    timestamp = this[HwmTable.timestamp],
    updatedTimestamp = this[HwmTable.updatedTimestamp]
)

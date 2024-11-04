package no.nav.paw.kafkakeymaintenance.perioder

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PerioderTable: Table("perioder") {
    val version = integer("version")
    val periodeId = uuid("periode_id")
    val identitetsnummer = varchar("identitetsnummer", 11)
    val fra = timestamp("fra")
    val til = timestamp("til").nullable()

    override val primaryKey = PrimaryKey(version, periodeId)
}
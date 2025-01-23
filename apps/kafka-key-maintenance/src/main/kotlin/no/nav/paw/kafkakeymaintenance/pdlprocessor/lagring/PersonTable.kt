package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PersonTable: Table("person_table") {
    val id = long("id").autoIncrement()
    val recordKey = varchar("record_key", 20)
    val versjon = integer("versjon")
    val sistEndret = timestamp("sist_endret")
    val tidspunktFraKilde = timestamp("tidspunkt_fra_kilde")
    val traceparent = varchar("traceparent", 55)
    val mergeProsessert = bool("merge_prosessert")
    override val primaryKey = PrimaryKey(id)
}
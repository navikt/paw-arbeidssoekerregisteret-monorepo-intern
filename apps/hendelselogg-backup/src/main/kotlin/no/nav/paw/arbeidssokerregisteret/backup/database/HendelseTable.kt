package no.nav.paw.arbeidssokerregisteret.backup.database

import org.jetbrains.exposed.sql.Table

object HendelseTable: Table() {
    val partition = integer("partition")
    val offset = long("offset")
    val recordKey = long("record_key")
    val arbeidssoekerId = long("arbeidssoeker_id")
    val data = binary("data")
    override val primaryKey: PrimaryKey = PrimaryKey(partition, offset)
}
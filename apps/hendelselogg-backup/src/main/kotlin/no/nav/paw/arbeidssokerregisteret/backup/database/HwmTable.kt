package no.nav.paw.arbeidssokerregisteret.backup.database

import org.jetbrains.exposed.sql.Table

object HwmTable: Table() {
    val partition = integer("id")
    val offset = long("offset")
    override val primaryKey: PrimaryKey = PrimaryKey(partition)
}
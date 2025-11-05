package no.nav.paw.kafkakeygenerator.model.dao

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

object KonfliktIdentiteterTable : LongIdTable("konflikt_identiteter") {
    val konfliktId = long("konflikt_id")
    val identitet = varchar("identitet", 50)
    val type = enumerationByName<IdentitetType>("type", 50)
    val gjeldende = bool("gjeldende")
    val insertedTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()

    fun findByAktorId(
        aktorId: String
    ): List<KonfliktIdentitetRow> = transaction {
        KonflikterTable.join(
            KonfliktIdentiteterTable,
            JoinType.LEFT,
            KonflikterTable.id,
            konfliktId
        ).selectAll()
            .where { KonflikterTable.aktorId eq aktorId }
            .map { it.asKonfliktIdentitetRow() }
    }

    fun insert(
        konfliktId: Long,
        identitet: Identitet
    ): Int = transaction {
        insert {
            it[KonfliktIdentiteterTable.konfliktId] = konfliktId
            it[KonfliktIdentiteterTable.identitet] = identitet.identitet
            it[KonfliktIdentiteterTable.type] = identitet.type
            it[KonfliktIdentiteterTable.gjeldende] = identitet.gjeldende
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun updateByKonfliktIdAndIdentitet(
        konfliktId: Long,
        identitet: Identitet
    ): Int = transaction {
        update(where = {
            (KonfliktIdentiteterTable.konfliktId eq konfliktId) and
                    (KonfliktIdentiteterTable.identitet eq identitet.identitet) and
                    (gjeldende neq identitet.gjeldende)
        }) {
            it[KonfliktIdentiteterTable.gjeldende] = identitet.gjeldende
            it[insertedTimestamp] = Instant.now()
        }
    }

    fun deleteByKonfliktIdAndIdentitet(
        konfliktId: Long,
        identitet: String
    ): Int = transaction {
        deleteWhere {
            KonfliktIdentiteterTable.id eq konfliktId
            KonfliktIdentiteterTable.identitet eq identitet
        }
    }
}
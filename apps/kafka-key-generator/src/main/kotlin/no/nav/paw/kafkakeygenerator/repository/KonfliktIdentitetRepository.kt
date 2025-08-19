package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.database.KonfliktIdentiteterTable
import no.nav.paw.kafkakeygenerator.database.KonflikterTable
import no.nav.paw.kafkakeygenerator.model.KonfliktIdentitetRow
import no.nav.paw.kafkakeygenerator.model.asKonfliktIdentitetRow
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class KonfliktIdentitetRepository {
    fun getById(
        id: Long,
    ): KonfliktIdentitetRow? = transaction {
        KonfliktIdentiteterTable.selectAll()
            .where { KonfliktIdentiteterTable.id eq id }
            .map { it.asKonfliktIdentitetRow() }
            .singleOrNull()
    }

    fun findByAktorId(
        aktorId: String
    ): List<KonfliktIdentitetRow> = transaction {
        KonflikterTable.join(
            KonfliktIdentiteterTable,
            JoinType.LEFT,
            KonflikterTable.id,
            KonfliktIdentiteterTable.konfliktId
        ).selectAll()
            .where { KonflikterTable.aktorId eq aktorId }
            .map { it.asKonfliktIdentitetRow() }
    }

    fun insert(
        konfliktId: Long,
        identitet: Identitet
    ): Int = transaction {
        KonfliktIdentiteterTable.insert {
            it[KonfliktIdentiteterTable.konfliktId] = konfliktId
            it[KonfliktIdentiteterTable.identitet] = identitet.identitet
            it[KonfliktIdentiteterTable.type] = identitet.type
            it[KonfliktIdentiteterTable.gjeldende] = identitet.gjeldende
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    fun deleteByKonfliktIdAndIdentitet(
        konfliktId: Long,
        identitet: String
    ): Int = transaction {
        KonfliktIdentiteterTable.deleteWhere {
            KonfliktIdentiteterTable.id eq konfliktId
            KonfliktIdentiteterTable.identitet eq identitet
        }
    }
}
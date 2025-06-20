package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.database.KonfliktIdentiteterTable
import no.nav.paw.kafkakeygenerator.model.KonfliktIdentitetRow
import no.nav.paw.kafkakeygenerator.model.asKonfliktIdentitetRow
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
}
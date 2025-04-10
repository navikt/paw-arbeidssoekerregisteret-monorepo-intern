package no.nav.paw.arbeidssoekerregisteret.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.model.EksterneVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.EksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.InsertEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.UpdateEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.asEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asSortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class EksterntVarselRepository {

    @WithSpan("EksterntVarselRepository.findAll")
    fun findAll(paging: Paging = Paging.none()): List<EksterntVarselRow> = transaction {
        EksterneVarslerTable.selectAll()
            .orderBy(EksterneVarslerTable.hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asEksterntVarselRow() }
    }

    @WithSpan("EksterntVarselRepository.findByVarselId")
    fun findByVarselId(varselId: UUID): EksterntVarselRow? = transaction {
        EksterneVarslerTable.selectAll()
            .where { EksterneVarslerTable.varselId eq varselId }
            .map { it.asEksterntVarselRow() }
            .firstOrNull()
    }

    @WithSpan("EksterntVarselRepository.insert")
    fun insert(varsel: InsertEksterntVarselRow): Int = transaction {
        EksterneVarslerTable.insert {
            it[varselId] = varsel.varselId
            it[varselType] = varsel.varselType
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    @WithSpan("EksterntVarselRepository.update")
    fun update(varsel: UpdateEksterntVarselRow): Int = transaction {
        EksterneVarslerTable.update({
            EksterneVarslerTable.varselId eq varsel.varselId
        }) {
            it[varselType] = varsel.varselType
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }

    @WithSpan("EksterntVarselRepository.deleteByUpdatedTimestampAndStatus")
    fun deleteByUpdatedTimestampAndStatus(
        updateTimestamp: Instant,
        vararg varselStatus: VarselStatus
    ): Int = transaction {
        EksterneVarslerTable.deleteWhere {
            (EksterneVarslerTable.updatedTimestamp less updateTimestamp) and
                    (EksterneVarslerTable.varselStatus inList varselStatus.toList())
        }
    }
}
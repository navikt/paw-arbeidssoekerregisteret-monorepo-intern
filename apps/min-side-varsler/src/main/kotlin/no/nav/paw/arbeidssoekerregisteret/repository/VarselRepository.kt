package no.nav.paw.arbeidssoekerregisteret.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.model.EksterneVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.InsertVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.UpdateVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselRow
import no.nav.paw.arbeidssoekerregisteret.model.VarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.asSortOrder
import no.nav.paw.arbeidssoekerregisteret.model.asVarselRow
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*

class VarselRepository {

    @WithSpan("findAll")
    fun findAll(paging: Paging = Paging.none()): List<VarselRow> = transaction {
        VarslerTable.join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .orderBy(VarslerTable.hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    @WithSpan("findByVarselId")
    fun findByVarselId(varselId: UUID): VarselRow? = transaction {
        VarslerTable.join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .where { VarslerTable.varselId eq varselId }
            .map { it.asVarselRow() }
            .firstOrNull()
    }

    @WithSpan("findByVarselId")
    fun findByBekreftelseId(bekreftelseId: UUID): VarselRow? = transaction {
        VarslerTable.join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .where { VarslerTable.bekreftelseId eq bekreftelseId }
            .map { it.asVarselRow() }
            .firstOrNull()
    }

    @WithSpan("findByPeriodeId")
    fun findByPeriodeId(
        periodeId: UUID,
        paging: Paging = Paging.none(),
    ): List<VarselRow> = transaction {
        VarslerTable.join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .where { VarslerTable.periodeId eq periodeId }
            .orderBy(VarslerTable.hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    @WithSpan("findByPeriodeIdAndVarselKilde")
    fun findByPeriodeIdAndVarselKilde(
        periodeId: UUID,
        varselKilde: VarselKilde,
        paging: Paging = Paging.none(),
    ): List<VarselRow> = transaction {
        VarslerTable.join(EksterneVarslerTable, JoinType.LEFT, VarslerTable.varselId, EksterneVarslerTable.varselId)
            .selectAll()
            .where { (VarslerTable.periodeId eq periodeId) and (VarslerTable.varselKilde eq varselKilde) }
            .orderBy(VarslerTable.hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asVarselRow() }
    }

    @WithSpan("insert")
    fun insert(varsel: InsertVarselRow): Int = transaction {
        VarslerTable.insert {
            it[periodeId] = varsel.periodeId
            it[bekreftelseId] = varsel.bekreftelseId
            it[varselId] = varsel.varselId
            it[varselKilde] = varsel.varselKilde
            it[varselType] = varsel.varselType
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[insertedTimestamp] = Instant.now()
        }.insertedCount
    }

    @WithSpan("update")
    fun update(varsel: UpdateVarselRow): Int = transaction {
        VarslerTable.update({
            VarslerTable.varselId eq varsel.varselId
        }) {
            it[varselStatus] = varsel.varselStatus
            it[hendelseNavn] = varsel.hendelseName
            it[hendelseTimestamp] = varsel.hendelseTimestamp
            it[updatedTimestamp] = Instant.now()
        }
    }
}
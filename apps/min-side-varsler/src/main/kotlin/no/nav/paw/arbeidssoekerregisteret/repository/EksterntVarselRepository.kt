package no.nav.paw.arbeidssoekerregisteret.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.model.EksterneVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.EksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.InsertEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.model.UpdateEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asEksterntVarselRow
import no.nav.paw.arbeidssoekerregisteret.model.asSortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

class EksterntVarselRepository {

    @WithSpan("findAll")
    fun findAll(paging: Paging = Paging.none()): List<EksterntVarselRow> = transaction {
        EksterneVarslerTable.selectAll()
            .orderBy(EksterneVarslerTable.hendelseTimestamp, paging.order.asSortOrder())
            .offset(paging.offset).limit(paging.size)
            .map { it.asEksterntVarselRow() }
    }

    @WithSpan("findByVarselId")
    fun findByVarselId(varselId: UUID): EksterntVarselRow? = transaction {
        EksterneVarslerTable.selectAll()
            .where { EksterneVarslerTable.varselId eq varselId }
            .map { it.asEksterntVarselRow() }
            .firstOrNull()
    }

    @WithSpan("insert")
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

    @WithSpan("update")
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
}
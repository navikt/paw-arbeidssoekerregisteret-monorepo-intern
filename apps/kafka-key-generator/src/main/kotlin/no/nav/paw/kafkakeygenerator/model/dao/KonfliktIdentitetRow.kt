package no.nav.paw.kafkakeygenerator.model.dao

import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.model.dao.KonfliktIdentiteterTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant

data class KonfliktIdentitetRow(
    val id: Long,
    val konfliktId: Long,
    val identitet: String,
    val type: IdentitetType,
    val gjeldende: Boolean,
    val insertedTimestamp: Instant,
    val updatedTimestamp: Instant? = null,
)

fun ResultRow.asKonfliktIdentitetRow(): KonfliktIdentitetRow = KonfliktIdentitetRow(
    id = this[KonfliktIdentiteterTable.id].value,
    konfliktId = this[KonfliktIdentiteterTable.konfliktId],
    identitet = this[KonfliktIdentiteterTable.identitet],
    type = this[KonfliktIdentiteterTable.type],
    gjeldende = this[KonfliktIdentiteterTable.gjeldende],
    insertedTimestamp = this[KonfliktIdentiteterTable.insertedTimestamp],
    updatedTimestamp = this[KonfliktIdentiteterTable.updatedTimestamp]
)
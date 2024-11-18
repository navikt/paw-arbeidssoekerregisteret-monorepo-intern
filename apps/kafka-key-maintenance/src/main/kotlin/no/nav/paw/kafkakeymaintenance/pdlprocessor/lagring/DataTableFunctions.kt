package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant

fun TransactionContext.insertOrUpdate(
    key: String,
    timestamp: Instant,
    traceparant: ByteArray?,
    data: ByteArray
) {
    if (hasId(key)) {
        DataTable.update(
            where = {
                (DataTable.version eq consumerVersion) and (DataTable.id eq key)
            }
        ) {
            it[DataTable.traceparant] = traceparant
            it[DataTable.data] = data
        }
    } else {
        DataTable.insert {
            it[version] = consumerVersion
            it[id] = key
            it[time] = timestamp
            it[DataTable.traceparant] = traceparant
            it[DataTable.data] = data
        }
    }
}

fun TransactionContext.getBatch(size: Int, time: Instant): List<Data> {
    return DataTable.selectAll()
        .where {
            (DataTable.version eq consumerVersion) and (DataTable.time lessEq time)
        }.orderBy(DataTable.time, SortOrder.ASC)
        .limit(size)
        .map {
            Data(
                id = it[DataTable.id],
                traceparant = it[DataTable.traceparant],
                time = it[DataTable.time],
                data = it[DataTable.data]
            )
        }
}

fun TransactionContext.delete(key: String): Boolean {
    return DataTable.deleteWhere {
        (version eq consumerVersion) and (id eq key)
    } > 0
}

fun TransactionContext.hasId(id: String): Boolean {
    return DataTable.select(DataTable.id)
        .where {
            (DataTable.version eq consumerVersion) and (DataTable.id eq id)
        }.count() > 0
}


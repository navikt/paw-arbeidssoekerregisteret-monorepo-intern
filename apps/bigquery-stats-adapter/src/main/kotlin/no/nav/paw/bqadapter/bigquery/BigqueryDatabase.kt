package no.nav.paw.bqadapter.bigquery

import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.Table
import no.nav.paw.bqadapter.Database

class BigqueryDatabase(
    private val bigqueryTables: Map<TableName, Table>
) : Database {

    fun write(tableName: TableName, rows: Iterable<Row>) {
        val tableId = bigqueryTables[tableName] ?: throw IllegalArgumentException("Table '$tableName' not found")
        tableId.insert(
            rows.map {
                row -> InsertAllRequest.RowToInsert.of(row.id, row.value)
            }
        )
    }

}

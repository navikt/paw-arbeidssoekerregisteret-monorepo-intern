package no.nav.paw.bqadapter.bigquery

import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.Table
import no.nav.paw.bqadapter.appLogger

class BigqueryDatabase(
    private val bigqueryTables: Map<TableName, Table>
) {
    fun write(tableName: TableName, rows: Iterable<Row>) {
        val tableId = bigqueryTables[tableName]
        if (tableId == null) {
            appLogger.error("Table $tableName not found in BigQuery")
            throw IllegalStateException("Table $tableName not found in BigQuery")
        }
        appLogger.info("Writing to table $tableName => $rows")
        val response = tableId.insert(
            rows.map {
                row -> InsertAllRequest.RowToInsert.of(row.id, row.value)
            }
        )
        if (response.hasErrors()) {
            appLogger.error("Failed to insert rows into table $tableName: ${response.insertErrors}")
            throw IllegalStateException("Could not insert rows into table " +
                    "$tableName: ${response.insertErrors.map { "${it.key}=${it.value}" }}")
        } else {
            appLogger.info("Inserted ${rows.count()} rows into table $tableName")
        }
    }
}

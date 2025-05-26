package no.nav.paw.bqadapter.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo

class BigQueryAdmin(
    private val bigQuery: BigQuery,
    private val project: String
) {

    fun getOrCreateTable(
        datasetName: DatasetName,
        tableName: TableName,
        schema: Schema
    ): Table {
        val tableId = TableId.of(project, datasetName.value, tableName.value)
        return bigQuery.getTable(tableId) ?: createTable(datasetName, tableName, schema)
    }

    fun createTable(datasetName: DatasetName, tableName: TableName, schema: Schema ): Table {
        val tableId = TableId.of(project, datasetName.value, tableName.value)
        val tableDefinition = StandardTableDefinition.of(schema)
        val tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
        return bigQuery.create(tableInfo)
    }

    fun getOrCreate(tableInfo: TableInfo): Table {
        val tableId = tableInfo.tableId
        return bigQuery.getTable(tableId) ?: bigQuery.create(tableInfo)
    }
}

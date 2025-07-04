package no.nav.paw.bqadapter.bigquery

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.bigquery.Bigquery
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import no.nav.paw.bqadapter.appLogger
import com.google.api.services.bigquery.model.Table as ModelTable

class BigQueryAdmin(
    private val bigQuery: BigQuery,
    private val bigquery: Bigquery,
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

    fun createTable(datasetName: DatasetName, tableName: TableName, schema: Schema): Table {
        val tableId = TableId.of(project, datasetName.value, tableName.value)
        val tableDefinition = StandardTableDefinition.of(schema)
        val tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
        return bigQuery.create(tableInfo)
    }

    fun getOrCreate(table: ModelTable): ModelTable {
        return bigquery.tables().get(
            project,
            table.tableReference.datasetId,
            table.tableReference.tableId
        ).let { request ->
            runCatching { request.execute() }
                .recover { ex ->
                    if (ex is GoogleJsonResponseException && ex.statusCode == 404) null
                    else throw ex
                }.getOrThrow()
                ?: bigquery.tables()
                    .insert(project, table.tableReference.datasetId, table)
                    .execute()
        }
    }
}

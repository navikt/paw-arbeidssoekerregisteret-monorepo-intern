package no.nav.paw.bqadapter.bigquery

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.bigquery.Bigquery
import com.google.api.services.bigquery.model.MaterializedViewDefinition
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
    ): Table = logBigQueryOperation("get-or-create table ${datasetName.value}.${tableName.value}") {
        val tableId = TableId.of(project, datasetName.value, tableName.value)
        bigQuery.getTable(tableId) ?: createTable(datasetName, tableName, schema)
    }

    fun createTable(datasetName: DatasetName, tableName: TableName, schema: Schema): Table {
        val tableId = TableId.of(project, datasetName.value, tableName.value)
        val tableDefinition = StandardTableDefinition.of(schema)
        val tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
        return bigQuery.create(tableInfo)
    }

    fun createOrUpdateMaterializedView(table: ModelTable): ModelTable =
        logBigQueryOperation(
            "create-or-update materialized view " +
                "${table.tableReference.datasetId}.${table.tableReference.tableId}"
        ) {
            val tableReference = table.tableReference
            val request = bigquery.tables().get(
                project,
                tableReference.datasetId,
                tableReference.tableId
            )
            val existingTable = try {
                request.execute()
            } catch (exception: GoogleJsonResponseException) {
                if (exception.statusCode == 404) null else throw exception
            }

            if (existingTable == null) {
                bigquery.tables()
                    .insert(project, tableReference.datasetId, table)
                    .execute()
            } else {
                updateMaterializedViewSettings(existingTable, table)
            }
        }

    private fun updateMaterializedViewSettings(existingTable: ModelTable, desiredTable: ModelTable): ModelTable {
        checkNotNull(existingTable.materializedView) {
            val tableReference = existingTable.tableReference
            "${tableReference.datasetId}.${tableReference.tableId} finnes, men er ikke et materialized view"
        }

        if (!materializedViewSettingsDiffer(existingTable, desiredTable)) {
            return existingTable
        }

        val tableReference = desiredTable.tableReference
        appLogger.info(
            "Oppdaterer refresh-innstillinger for {}.{}",
            tableReference.datasetId,
            tableReference.tableId
        )
        return bigquery.tables()
            .patch(
                project,
                tableReference.datasetId,
                tableReference.tableId,
                materializedViewSettingsPatch(desiredTable)
            )
            .execute()
    }
}

internal fun materializedViewSettingsDiffer(existingTable: ModelTable, desiredTable: ModelTable): Boolean {
    val existingDefinition = checkNotNull(existingTable.materializedView)
    val desiredDefinition = checkNotNull(desiredTable.materializedView)
    return existingDefinition.enableRefresh != desiredDefinition.enableRefresh ||
        existingDefinition.refreshIntervalMs != desiredDefinition.refreshIntervalMs ||
        existingTable.maxStaleness != desiredTable.maxStaleness
}

internal fun materializedViewSettingsPatch(desiredTable: ModelTable): ModelTable {
    val desiredDefinition = checkNotNull(desiredTable.materializedView)
    return ModelTable()
        .setMaterializedView(
            MaterializedViewDefinition()
                .setEnableRefresh(desiredDefinition.enableRefresh)
                .setRefreshIntervalMs(desiredDefinition.refreshIntervalMs)
        )
        .setMaxStaleness(desiredTable.maxStaleness)
}

internal inline fun <T> logBigQueryOperation(name: String, operation: () -> T): T {
    val startedAt = System.nanoTime()
    appLogger.info("Starter BigQuery-operasjon: {}", name)
    try {
        return operation().also {
            appLogger.info(
                "Fullførte BigQuery-operasjon: {} etter {} ms",
                name,
                (System.nanoTime() - startedAt) / 1_000_000
            )
        }
    } catch (exception: Exception) {
        appLogger.error(
            "BigQuery-operasjon feilet: $name etter " +
                "${(System.nanoTime() - startedAt) / 1_000_000} ms",
            exception
        )
        throw exception
    }
}

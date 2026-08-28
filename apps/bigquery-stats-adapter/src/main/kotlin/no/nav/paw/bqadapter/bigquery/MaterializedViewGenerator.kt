package no.nav.paw.bqadapter.bigquery

import com.google.api.services.bigquery.model.MaterializedViewDefinition
import com.google.api.services.bigquery.model.Table
import com.google.api.services.bigquery.model.TableReference
import no.nav.paw.bqadapter.appLogger
import java.io.File
import java.time.Duration

const val views_path = "materialized_views/"
val materializedViewsRefreshInterval = Duration.ofHours(6)
val materializedViewsMaxStaleness = Duration.ofHours(7)

@JvmInline
value class Sql(val value: String)
data class View<A>(
    val name: String,
    val representation: A
)

fun BigQueryAdmin.createMaterializedViews(
    datasetName: DatasetName,
    path: String
): List<String> {
    val views = viewsFromResource(path).orEmpty()
    appLogger.info("Fant {} materialized views i {}", views.size, path)
    return views
        .map { createMaterializedViewDefinition(datasetName, it) }
        .map { table ->
            createOrUpdateMaterializedView(table)
            "${table.tableReference.datasetId}.${table.tableReference.tableId}"
        }
}

fun viewsFromResource(path: String): List<View<Sql>>? =
    Thread.currentThread()
        .contextClassLoader.getResource(path)
        ?.let { File(it.toURI()) }
        ?.listFiles()
        ?.map { file ->
            val name = file.nameWithoutExtension
            val sql = Sql(file.readText())
            View(name, sql)
        }

fun createMaterializedViewDefinition(datasetName: DatasetName, view: View<Sql>): Table {
    val tableRef = TableReference().apply {
        tableId = view.name
        datasetId = datasetName.value
    }
    val viewDefinition = MaterializedViewDefinition()
        .setQuery(view.representation.value)
        .setEnableRefresh(true)
        .setRefreshIntervalMs(materializedViewsRefreshInterval.toMillis())
        .setAllowNonIncrementalDefinition(true)
    return Table().apply {
        tableReference = tableRef
        materializedView = viewDefinition
    }.setMaxStaleness("0-0 0 ${materializedViewsMaxStaleness.toHours()}:0:0")
}

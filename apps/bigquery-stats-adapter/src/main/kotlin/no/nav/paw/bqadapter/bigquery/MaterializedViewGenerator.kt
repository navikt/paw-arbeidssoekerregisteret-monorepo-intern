package no.nav.paw.bqadapter.bigquery

import com.google.api.services.bigquery.model.MaterializedViewDefinition
import com.google.api.services.bigquery.model.Table
import com.google.api.services.bigquery.model.TableReference
import java.io.File
import java.time.Duration

const val views_path = "materialized_views/"
val matrialized_views_refresh_interval = Duration.ofHours(6)
val matrialized_views_max_staleness = Duration.ofHours(7)

@JvmInline
value class Sql(val value: String)
data class View<A>(
    val name: String,
    val representation: A
)

fun BigQueryAdmin.createMaterializedViews(
    datasetName: DatasetName,
    path: String
): List<String> =
    viewsFromResource(path)
        ?.map { createMaterializedViewDefinition(datasetName, it) }
        ?.map { table ->
            getOrCreate(table)
            "${table.tableReference.datasetId}.${table.tableReference.tableId}"
        } ?: emptyList()

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
        .setRefreshIntervalMs(matrialized_views_refresh_interval.toMillis())
        .setAllowNonIncrementalDefinition(true)
    return Table().apply {
        tableReference = tableRef
        materializedView = viewDefinition
    }.setMaxStaleness("0-0 0 ${matrialized_views_max_staleness.toHours()}:0:0")
}

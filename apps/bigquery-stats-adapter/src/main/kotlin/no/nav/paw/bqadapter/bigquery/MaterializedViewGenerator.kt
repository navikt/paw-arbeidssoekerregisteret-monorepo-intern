package no.nav.paw.bqadapter.bigquery

import com.google.api.services.bigquery.model.MaterializedViewDefinition
import com.google.api.services.bigquery.model.Table
import com.google.api.services.bigquery.model.TableReference
import java.io.File
import java.time.Duration

const val views_path = "materialized_views/"
val matrialized_views_refresh_interval = Duration.ofHours(6)

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
        ?.map { view ->
            getOrCreate(datasetName, view.representation)
            "${datasetName}.${view.name}"
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

fun createMaterializedViewDefinition(datasetName: DatasetName, view: View<Sql>): View<Table> {
    val tableRef = TableReference().apply {
        tableId = view.name
        datasetId = datasetName.value
    }
    val viewDefinition = MaterializedViewDefinition()
        .setQuery(view.representation.value)
        .setEnableRefresh(false)
        .setRefreshIntervalMs(matrialized_views_refresh_interval.toMillis())
        .setAllowNonIncrementalDefinition(true)
        .encodeMaxStaleness(
            "INTERVAL \"${matrialized_views_refresh_interval.toHours()}\" HOUR"
                .toByteArray()
        )
    val table = Table().apply {
        tableReference = tableRef
        materializedView = viewDefinition
    }
    return View(
        name = view.name,
        representation = table
    )
}

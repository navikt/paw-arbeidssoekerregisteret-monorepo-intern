package no.nav.paw.bqadapter.bigquery

import com.google.cloud.bigquery.MaterializedViewDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import java.io.File
import java.time.Duration

const val views_path = "materialized_views/"
val matrialized_views_refresh_interval_ms = Duration.ofHours(6).toMillis()

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
        ?.map(::createMaterializedViewDefinition)
        ?.map { view ->
            val tableId = TableId.of(datasetName.value, view.name)
            val tableInfo = TableInfo.of(tableId, view.representation)
            getOrCreate(tableInfo)
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


fun createMaterializedViewDefinition(view: View<Sql>): View<MaterializedViewDefinition> =
    View(
        name = view.name,
        representation =
            MaterializedViewDefinition
                .newBuilder(view.representation.value)
                .setEnableRefresh(false)
                .setRefreshIntervalMs(matrialized_views_refresh_interval_ms)
                .build()
    )

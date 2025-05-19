package no.nav.paw.bqadapter.bigquery

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Table
import no.nav.paw.bqadapter.bigquery.schema.hendelserSchema
import no.nav.paw.bqadapter.bigquery.schema.perioderSchema

private val INTERNT_DATASET = DatasetName("arbeidssoekerregisteret_internt")
private val GRAFANA_DATASET = DatasetName("arbeidssoekerregisteret_grafana")

private val PERIODE_TABELL = TableName("perioder")
private val HENDELSE_TABELL = TableName("hendelser")

class BigQueryContext(
    val tables: Map<TableName, Table>,
    val bqAdmin: BigQueryAdmin,
    val bqDatabase: BigqueryDatabase
)

fun createBigQueryContext(project: String): BigQueryContext {
    val bigquery =  BigQueryOptions.getDefaultInstance().getService();
    val bqAdmin = BigQueryAdmin(
        bigQuery = bigquery,
        project = project
    )

    val tables = mapOf(
        PERIODE_TABELL to bqAdmin.getOrCreateTable(
            datasetName = INTERNT_DATASET,
            tableName = PERIODE_TABELL,
            schema = perioderSchema
        ),
        HENDELSE_TABELL to bqAdmin.getOrCreateTable(
            datasetName = INTERNT_DATASET,
            tableName = HENDELSE_TABELL,
            schema = hendelserSchema
        )
    )

    return BigQueryContext(
        tables = tables,
        bqAdmin = bqAdmin,
        bqDatabase = BigqueryDatabase(bigqueryTables = tables)
    )
}
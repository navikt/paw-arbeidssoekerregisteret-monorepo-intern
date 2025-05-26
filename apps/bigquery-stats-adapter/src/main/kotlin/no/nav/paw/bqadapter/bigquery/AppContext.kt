package no.nav.paw.bqadapter.bigquery

import com.google.cloud.bigquery.BigQuery
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.bqadapter.Encoder
import no.nav.paw.bqadapter.bigquery.schema.hendelserSchema
import no.nav.paw.bqadapter.bigquery.schema.perioderSchema
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import org.apache.kafka.common.serialization.Deserializer

private val INTERNT_DATASET = DatasetName("arbeidssoekerregisteret_internt")
private val GRAFANA_DATASET = DatasetName("arbeidssoekerregisteret_grafana")

val PERIODE_TABELL = TableName("perioder")
val HENDELSE_TABELL = TableName("hendelser")

class AppContext(
    val bqDatabase: BigqueryDatabase,
    val encoder: Encoder,
    val hendelseDeserializer: HendelseDeserializer,
    val periodeDeserializer: Deserializer<Periode>,
    val livenessHealthIndicator: LivenessHealthIndicator,
    val readinessHealthIndicator: ReadinessHealthIndicator
)

fun initBqApp(
    bigquery: BigQuery,
    project: String,
    encoder: Encoder,
    hendelserDeserializer: HendelseDeserializer,
    periodeDeserializer: Deserializer<Periode>,
    livenessHealthIndicator: LivenessHealthIndicator,
    readinessHealthIndicator: ReadinessHealthIndicator
): AppContext {
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

    bqAdmin.createMaterializedViews(
        datasetName = GRAFANA_DATASET,
        path = views_path
    )

    return AppContext(
        livenessHealthIndicator = livenessHealthIndicator,
        readinessHealthIndicator = readinessHealthIndicator,
        bqDatabase = BigqueryDatabase(bigqueryTables = tables),
        encoder = encoder,
        hendelseDeserializer = hendelserDeserializer,
        periodeDeserializer = periodeDeserializer
    )
}
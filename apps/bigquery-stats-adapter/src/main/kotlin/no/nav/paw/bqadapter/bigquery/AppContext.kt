package no.nav.paw.bqadapter.bigquery

import com.google.api.services.bigquery.Bigquery
import com.google.cloud.bigquery.BigQuery
import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bqadapter.Encoder
import no.nav.paw.bqadapter.appLogger
import no.nav.paw.bqadapter.bigquery.schema.hendelserSchema
import no.nav.paw.bqadapter.bigquery.schema.perioderSchema
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.kafka.factory.KafkaFactory
import org.apache.kafka.common.serialization.Deserializer

private val INTERNT_DATASET = DatasetName("arbeidssoekerregisteret_internt")
private val GRAFANA_DATASET = DatasetName("arbeidssoekerregisteret_grafana")

val PERIODE_TABELL = TableName("perioder")
val HENDELSE_TABELL = TableName("hendelser")

class AppContext(
    val bqDatabase: BigqueryDatabase,
    val encoder: Encoder,
    val deserializers: Deserializers,
    val livenessHealthIndicator: LivenessHealthIndicator,
    val readinessHealthIndicator: ReadinessHealthIndicator,
    val topics: TopicNames
)

class Deserializers(
    val hendelseDeserializer: HendelseDeserializer,
    val periodeDeserializer: Deserializer<Periode>,
    val bekreftelseDeserializer: Deserializer<Bekreftelse>,
    val påVegneAvDeserializers: Deserializer<PaaVegneAv>
)

fun KafkaFactory.deserializers(): Deserializers = Deserializers(
    hendelseDeserializer = HendelseDeserializer(),
    periodeDeserializer = kafkaAvroDeSerializer(),
    bekreftelseDeserializer = kafkaAvroDeSerializer(),
    påVegneAvDeserializers = kafkaAvroDeSerializer()
)

fun initBqApp(
    bigquery: BigQuery,
    bigqueryModel: Bigquery,
    project: String,
    encoder: Encoder,
    deserializers: Deserializers,
    livenessHealthIndicator: LivenessHealthIndicator,
    readinessHealthIndicator: ReadinessHealthIndicator
): AppContext {
    val bqAdmin = BigQueryAdmin(
        bigQuery = bigquery,
        bigquery = bigqueryModel,
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

    val views =bqAdmin.createMaterializedViews(
        datasetName = GRAFANA_DATASET,
        path = views_path
    )
    appLogger.info("Materialized views created: ${views.joinToString(", ")}")

    return AppContext(
        livenessHealthIndicator = livenessHealthIndicator,
        readinessHealthIndicator = readinessHealthIndicator,
        bqDatabase = BigqueryDatabase(bigqueryTables = tables),
        encoder = encoder,
        deserializers = deserializers,
        topics = standardTopicNames()
    )
}
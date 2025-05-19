package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field.of
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.paw.bqadapter.bigquery.schema.structs.metadataStruct

val hendelserSchema: Schema get() = Schema.of(
    of ( "id", StandardSQLTypeName.STRING),
    of ( "correlation_id", StandardSQLTypeName.STRING),
    of ( "type", StandardSQLTypeName.STRING),
    of("metadata", StandardSQLTypeName.STRUCT, metadataStruct)
)


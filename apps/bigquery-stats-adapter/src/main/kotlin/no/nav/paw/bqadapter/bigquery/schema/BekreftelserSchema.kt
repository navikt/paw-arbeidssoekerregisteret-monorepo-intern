package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import no.nav.paw.bqadapter.bigquery.schema.structs.metadataStruct

val bekreftelseSchema: Schema
    get() = Schema.of(
        Field.of("id", StandardSQLTypeName.STRING),
        Field.of("periode_id", StandardSQLTypeName.STRING),
        Field.of("type", StandardSQLTypeName.STRING),
        Field.of("metadata", StandardSQLTypeName.STRUCT, metadataStruct)
)
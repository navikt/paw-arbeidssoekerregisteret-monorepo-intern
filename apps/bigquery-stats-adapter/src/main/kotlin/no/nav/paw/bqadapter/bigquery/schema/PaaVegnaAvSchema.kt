package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName

val paaVegnaAvSchema: Schema
    get() = Schema.of(
        Field.of("id", StandardSQLTypeName.STRING),
        Field.of("timestamp", StandardSQLTypeName.TIMESTAMP),
        Field.of("periode_id", StandardSQLTypeName.STRING),
        Field.of("loesning", StandardSQLTypeName.STRING)
)
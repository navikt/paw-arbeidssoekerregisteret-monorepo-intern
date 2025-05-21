package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.StandardSQLTypeName

fun String.ofRequiredType(type: StandardSQLTypeName): Field {
    val valid = this.ifBlank { throw IllegalArgumentException("Field name cannot be blank") }
    return Field.newBuilder(valid, type)
        .setMode(Field.Mode.REQUIRED)
        .build()
}

fun String.ofOptionalType(type: StandardSQLTypeName): Field {
    val valid = this.ifBlank { throw IllegalArgumentException("Field name cannot be blank") }
    return Field.newBuilder(valid, type)
        .setMode(Field.Mode.NULLABLE)
        .build()
}

fun String.ofRepeatedType(type: StandardSQLTypeName): Field {
    val valid = this.ifBlank { throw IllegalArgumentException("Field name cannot be blank") }
    return Field.newBuilder(valid, type)
        .setMode(Field.Mode.REPEATED)
        .build()
}

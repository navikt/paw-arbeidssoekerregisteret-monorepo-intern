package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.StandardSQLTypeName
import java.time.Instant
import java.time.LocalDate.ofInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

val localTimeZone = ZoneId.of("Europe/Oslo")
val dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun Instant.toBqDateString(): String =
    ofInstant(this, localTimeZone).format(dateTimeFormater)
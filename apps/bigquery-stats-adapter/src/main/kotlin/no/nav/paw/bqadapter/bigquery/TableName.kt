package no.nav.paw.bqadapter.bigquery

@JvmInline
value class TableName(val value: String) {
    init {
        require(value.isNotBlank()) { "Table name cannot be blank" }
    }
}
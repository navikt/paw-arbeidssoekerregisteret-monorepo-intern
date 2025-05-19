package no.nav.paw.bqadapter.bigquery

@JvmInline
value class DatasetName(val value: String) {
    init {
        require(value.isNotBlank()) { "Dataset name cannot be blank" }
    }
}
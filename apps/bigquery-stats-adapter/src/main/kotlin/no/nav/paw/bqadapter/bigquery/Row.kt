package no.nav.paw.bqadapter.bigquery

data class Row(
    val id: String,
    val value: Map<String, Any>
)
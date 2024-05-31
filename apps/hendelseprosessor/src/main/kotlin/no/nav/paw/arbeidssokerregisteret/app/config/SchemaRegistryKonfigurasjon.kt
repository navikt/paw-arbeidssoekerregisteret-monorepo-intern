package no.nav.paw.arbeidssokerregisteret.app.config

data class SchemaRegistryKonfigurasjon(
    val url: String,
    val bruker: String?,
    val passord: String?,
    val autoRegistrerSchema: Boolean = true)


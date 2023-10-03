package no.nav.paw.arbeidssokerregisteret.app.config

import no.nav.paw.arbeidssokerregisteret.app.config.helpers.konfigVerdi
import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_SCHEMA_REGISTRY
import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_SCHEMA_REGISTRY_PASSWORD
import no.nav.paw.arbeidssokerregisteret.app.config.nais.KAFKA_SCHEMA_REGISTRY_USER

class SchemaRegistryConfig(map: Map<String, String>) {
    val url: String = map.konfigVerdi(KAFKA_SCHEMA_REGISTRY)
    val bruker: String? = map.konfigVerdi(KAFKA_SCHEMA_REGISTRY_USER)
    val passord: String? = map.konfigVerdi(KAFKA_SCHEMA_REGISTRY_PASSWORD)
    val autoRegistrerSchema: Boolean = true
}
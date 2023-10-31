package no.nav.paw.arbeidssokerregisteret.app.config

data class KafkaServerKonfigurasjon(
    val autentisering: String,
    val kafkaBrokers: String,
    val keyStorePath: String?,
    val credStorePassword: String?,
    val truststorePath: String?
)
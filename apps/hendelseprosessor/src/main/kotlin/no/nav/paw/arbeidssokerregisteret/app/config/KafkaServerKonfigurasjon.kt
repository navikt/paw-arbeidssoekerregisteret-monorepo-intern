package no.nav.paw.arbeidssokerregisteret.app.config

data class KafkaServerKonfigurasjon(
    val autentisering: String,
    val kafkaBrokers: String,
    val keystorePath: String?,
    val credstorePassword: String?,
    val truststorePath: String?
)
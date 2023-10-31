package no.nav.paw.kafkakeygenerator.config


data class Autentiseringskonfigurasjon(
    val providers: List<Autentisering>,
    val kafkaKeyApiAuthProvider: String
)

data class Autentisering(
    val name: String,
    val discoveryUrl: String,
    val acceptedAudience: List<String>,
    val cookieName: String? = null,
    val requiredClaims: List<String>
)

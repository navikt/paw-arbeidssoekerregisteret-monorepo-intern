package no.nav.paw.kafkakeygenerator.api.oppslag

data class FailureResponseV1(
    val message: String,
    val code: Feilkode
)

enum class Feilkode {
    UKJENT_IDENT,
    UKJENT_REGISTERET,
    TEKNISK_FEIL
}
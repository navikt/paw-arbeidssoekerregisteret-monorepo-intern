package no.nav.paw.kafkakeygenerator.api.v1

data class FailureResponseV1(
    val message: String,
    val code: Feilkode
): RecordKeyResponse

enum class Feilkode {
    UKJENT_IDENT,
    UKJENT_REGISTERET,
    TEKNISK_FEIL
}
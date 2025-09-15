package no.nav.paw.kafkakeygenerator.model

enum class KafkaKeyStatus {
    OPPRETTET,
    OPPDATERT,
    VERIFISERT,
    KONFLIKT,
    IKKE_OPPRETTET,
    IKKE_OPPDATERT
}
package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.time.Instant

data class VarselHendelse(
    @JsonProperty("@event_name") val eventName: VarselEventName,
    val status: VarselStatus?,
    val varselId: String,
    val varseltype: VarselType,
    val kanal: VarselKanal?,
    val renotifikasjon: Boolean?,
    val sendtSomBatch: Boolean?,
    val feilmelding: String?,
    val namespace: String,
    val appnavn: String,
    val tidspunkt: Instant
)

enum class VarselEventName(@get:JsonValue val value: String) {
    OPPRETTET("opprettet"),
    AKTIVERT("aktivert"),
    INAKTIVERT("inaktivert"),
    SLETTET("slettet"),
    EKSTERN_STATUS_OPPDATERT("eksternStatusOppdatert"),

    @JsonEnumDefaultValue
    UKJENT("ukjent")
}

enum class VarselStatus(@get:JsonValue val value: String) {
    VENTER("venter"),
    BESTILT("bestilt"),
    SENDT("sendt"),
    FEILET("feilet"),
    KANSELLERT("kansellert"),
    FERDIGSTILT("ferdigstilt"),

    @JsonEnumDefaultValue
    UKJENT("ukjent")
}

enum class VarselType(@get:JsonValue val value: String) {
    BESKJED("beskjed"),
    OPPGAVE("oppgave"),
    INNBOKS("innboks"),

    @JsonEnumDefaultValue
    UKJENT("ukjent")
}

enum class VarselKanal(@get:JsonValue val value: String) {
    SMS("SMS"),
    EPOST("EPOST"),

    @JsonEnumDefaultValue
    UKJENT("UKJENT")
}

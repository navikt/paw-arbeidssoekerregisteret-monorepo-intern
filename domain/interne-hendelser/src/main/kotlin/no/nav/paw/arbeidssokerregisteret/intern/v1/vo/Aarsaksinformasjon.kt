package no.nav.paw.arbeidssokerregisteret.intern.v1.vo

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

data class Aarsaksinformasjon(
    val aarsak: AvsluttetAarsakType,
    val regelEvalResultat: RegelEvalResultat
)

enum class RegelEvalResultat {
    /** Revel eval resultat er ikke beregnet for denne årsaken.
     * Beregnes bare når perioden avsluttes direkte av veileder (ikke via bekreftels selv når den
     * leveres av veileder).
     */
    IKKE_RELEVANT,

    /**
     * Det er noe, men systemet klarte ikke å matche det mot en en kjent type.
     */
    UDEFINERT,

    /**
     * Evaluering feilet, så vi vet egentlig ikke noe
     */
    FEIL_UNDER_EVAL,
    DOED,
    SAVNET,
    OPPHOERT,
    EU_EOES_IKKE_BOSATT,
    NORSK_IKKE_BOSATT,
    IKKE_BOSATT,
    OK,
    @JsonEnumDefaultValue
    UKJENT_VERDI,
}

enum class AvsluttetAarsakType {

    /** Svarte eksplisitt "NEI" på å fortsette som arbeidssøker i bekreftelsen */
    SVARTE_NEI_I_BEKREFTELSE,

    /**
     * Svarte ikke på bekreftelsen innen fristen.
     * Gjelder uavhengig av om innsamlingen skjer via ekstern kanal eller ikke.
     */
    BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST,

    /**
    * Feilregistrering: Veileder har brukt slette funksjon,
     * perioden skal altså sees på som en feilregistrering.
    */
    FEILREGISTRERING,

    /** Årsaken er ikke kjent */
    UDEFINERT,

    /**
     * Settes aldri av produsenten!
     * En verdi er satt av produsenten, men denne versjonen av enumen kjenner den ikke.
     * Brukes ved deserialisering av hendelser produsert av en nyere versjon.
     */
    @JsonEnumDefaultValue
    UKJENT_VERDI,
}

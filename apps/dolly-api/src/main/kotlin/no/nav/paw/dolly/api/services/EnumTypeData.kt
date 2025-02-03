package no.nav.paw.dolly.api.services

import no.nav.paw.dolly.api.models.TypeRequest
import no.nav.paw.dolly.api.models.TypeResponseInner

object EnumTypeData {
    private val jobbsituasjonsbeskrivelser = mapOf(
        "UKJENT_VERDI" to "Ukjent verdi",
        "UDEFINERT" to "Udefinert",
        "HAR_SAGT_OPP" to "Har sagt opp jobben",
        "HAR_BLITT_SAGT_OPP" to "Har blitt sagt opp",
        "ER_PERMITTERT" to "Er permittert",
        "ALDRI_HATT_JOBB" to "Har aldri hatt jobb",
        "IKKE_VAERT_I_JOBB_SISTE_2_AAR" to "Ikke vært i jobb siste 2 år",
        "AKKURAT_FULLFORT_UTDANNING" to "Har akkurat fullført utdanning",
        "VIL_BYTTE_JOBB" to "Vil bytte jobb",
        "USIKKER_JOBBSITUASJON" to "Usikker jobbsituasjon",
        "MIDLERTIDIG_JOBB" to "Har midlertidig jobb",
        "DELTIDSJOBB_VIL_MER" to "Har deltidsjobb, vil ha mer",
        "NY_JOBB" to "Har fått ny jobb",
        "KONKURS" to "Har gått konkurs",
        "ANNET" to "Annet"
    )

    private val brukertyper = mapOf(
        "UKJENT_VERDI" to "Ukjent verdi",
        "UDEFINERT" to "Udefinert",
        "VEILEDER" to "Veileder",
        "SLUTTBRUKER" to "Sluttbruker",
        "SYSTEM" to "System"
    )

    fun hentEnumTypeResponse(type: TypeRequest): List<TypeResponseInner> = when (type) {
        TypeRequest.JOBBSITUASJONSBESKRIVELSE -> jobbsituasjonsbeskrivelser.map { (key, value) ->
            TypeResponseInner(
                key,
                value
            )
        }

        TypeRequest.BRUKERTYPE -> brukertyper.map { (key, value) -> TypeResponseInner(key, value) }
    }
}
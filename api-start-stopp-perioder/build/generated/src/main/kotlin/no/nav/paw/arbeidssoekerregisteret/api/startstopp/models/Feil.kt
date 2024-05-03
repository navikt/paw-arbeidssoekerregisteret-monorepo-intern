/**
* paw_arbeidssokerregisteret_api_start_stopp API
* paw_arbeidssokerregisteret_api_start_stopp API
*
* The version of the OpenAPI document: 1.0.0
* 
*
* NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
* https://openapi-generator.tech
* Do not edit the class manually.
*/
package no.nav.paw.arbeidssoekerregisteret.api.startstopp.models

import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.AarsakTilAvvisning

/**
 * 
 * @param melding 
 * @param feilKode 
 * @param aarsakTilAvvisning 
 */
data class Feil(
    val melding: kotlin.String,
    val feilKode: Feil.FeilKode,
    val aarsakTilAvvisning: AarsakTilAvvisning? = null
) 
{
    /**
    * 
    * Values: UKJENT_FEIL,UVENTET_FEIL_MOT_EKSTERN_TJENESTE,FEIL_VED_LESING_AV_FORESPORSEL,AVVIST,IKKE_TILGANG
    */
    enum class FeilKode(val value: kotlin.String){
        UKJENT_FEIL("UKJENT_FEIL"),
        UVENTET_FEIL_MOT_EKSTERN_TJENESTE("UVENTET_FEIL_MOT_EKSTERN_TJENESTE"),
        FEIL_VED_LESING_AV_FORESPORSEL("FEIL_VED_LESING_AV_FORESPORSEL"),
        AVVIST("AVVIST"),
        IKKE_TILGANG("IKKE_TILGANG");
    }
}


/**
* paw_arbeidssokerregisteret_api_inngang API
* paw_arbeidssokerregisteret_api_inngang API
*
* The version of the OpenAPI document: 1.0.0
* 
*
* NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
* https://openapi-generator.tech
* Do not edit the class manually.
*/
package no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models


/**
 * 
 * @param melding 
 * @param feilKode 
 */
data class Feil(
    val melding: kotlin.String,
    val feilKode: Feil.FeilKode
) 
{
    /**
    * 
    * Values: UKJENT_FEIL,UVENTET_FEIL_MOT_EKSTERN_TJENESTE,FEIL_VED_LESING_AV_FORESPORSEL,IKKE_TILGANG
    */
    enum class FeilKode(val value: kotlin.String){
        UKJENT_FEIL("UKJENT_FEIL"),
        UVENTET_FEIL_MOT_EKSTERN_TJENESTE("UVENTET_FEIL_MOT_EKSTERN_TJENESTE"),
        FEIL_VED_LESING_AV_FORESPORSEL("FEIL_VED_LESING_AV_FORESPORSEL"),
        IKKE_TILGANG("IKKE_TILGANG");
    }
}


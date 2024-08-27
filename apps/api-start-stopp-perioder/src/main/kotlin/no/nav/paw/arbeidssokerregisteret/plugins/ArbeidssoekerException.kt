package no.nav.paw.arbeidssokerregisteret.plugins

import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Feil

abstract class ArbeidssoekerException(
    val status: HttpStatusCode,
    description: String? = null,
    val feilkode: InternFeilkode,
    causedBy: Throwable? = null
) : Exception("Request failed with status: $status. Description: $description. ErrorCode: $feilkode", causedBy)

enum class InternFeilkode {
    UVENTET_FEIL_MOT_EKSTERN_TJENESTE

}

fun InternFeilkode.opplysningerFeilkode(): Feil.FeilKode {
    when (this) {
        InternFeilkode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE -> return Feil.FeilKode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE

    }
}

fun InternFeilkode.startStoppFeilkode(): no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2.FeilKode {
    when (this) {
        InternFeilkode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE -> return no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2.FeilKode.UVENTET_FEIL_MOT_EKSTERN_TJENESTE
    }
}
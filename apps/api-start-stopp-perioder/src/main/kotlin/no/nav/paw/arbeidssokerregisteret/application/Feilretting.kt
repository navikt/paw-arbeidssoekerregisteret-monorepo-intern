package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feilretting.FeilType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import java.time.Instant
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feilretting as ApiFeilretting

sealed interface Feilretting
sealed interface GyldigFeilretting: Feilretting

data class Feilregistrering(
    val melding: String? = null
): GyldigFeilretting

data class FeilTidspunkt(
    val melding: String?,
    val tidspunkt: Instant
): GyldigFeilretting

data class UgyldigFeilretting(val grunn: String) : Feilretting

val Feilretting?.tidspunkt: Instant? get() = when (this) {
    is FeilTidspunkt -> tidspunkt
    else -> null
}

val Feilretting?.aarsak get() = when (this) {
    is Feilregistrering -> "Feilregistrering${this.melding?.let { ": $it" } ?: ""}"
    is FeilTidspunkt -> "FeilTidspunkt${this.melding?.let { ": $it" } ?: ""}"
    else -> null
}

val Feilretting?.tidspunktFraKilde get() = when (this) {
    is FeilTidspunkt -> TidspunktFraKilde(
        tidspunkt = tidspunkt,
        avviksType = AvviksType.TIDSPUNKT_KORRIGERT
    )
    is Feilregistrering -> TidspunktFraKilde(
        tidspunkt = Instant.now(),
        avviksType = AvviksType.SLETTET
    )
    else -> null
}

fun feilretting(apiObj: ApiFeilretting?): Feilretting? {
    if (apiObj == null) return null
    when (apiObj.feilType) {
        FeilType.FeilTidspunkt -> {
            if (apiObj.tidspunkt == null) {
                return UgyldigFeilretting("FeilTidspunkt må ha tidspunkt")
            }
            return FeilTidspunkt(apiObj.melding, apiObj.tidspunkt)
        }
        FeilType.Feilregistrering -> {
            when {
                apiObj.tidspunkt != null -> {
                    return UgyldigFeilretting("Feilregistrering kan ikke ha tidspunkt")
                }
                else -> {
                    return Feilregistrering(apiObj.melding)
                }
            }
        }
    }
}


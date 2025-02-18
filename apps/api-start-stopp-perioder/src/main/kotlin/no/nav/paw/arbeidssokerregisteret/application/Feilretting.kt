package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feilretting.FeilType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import java.time.Instant
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feilretting as ApiFeilretting

sealed interface Feilretting
sealed interface GyldigFeilretting : Feilretting

data class Feilregistrering(
    val melding: String? = null
) : GyldigFeilretting

data class FeilTidspunkt(
    val melding: String?,
    val tidspunkt: Instant
) : GyldigFeilretting

data class UgyldigFeilretting(
    val grunn: String,
    val kilde: ApiFeilretting
) : Feilretting

val Feilretting?.tidspunkt: Instant?
    get() = when (this) {
        is FeilTidspunkt -> tidspunkt
        else -> null
    }

val Feilretting?.aarsak
    get() = when (this) {
        is Feilregistrering -> "Feilregistrering${this.melding?.let { ": $it" } ?: ""}"
        is FeilTidspunkt -> "FeilTidspunkt${this.melding?.let { ": $it" } ?: ""}"
        else -> null
    }

val Feilretting?.tidspunktFraKilde
    get() = when (this) {
        null -> null
        is FeilTidspunkt -> TidspunktFraKilde(
            tidspunkt = tidspunkt,
            avviksType = AvviksType.TIDSPUNKT_KORRIGERT
        )

        is Feilregistrering -> TidspunktFraKilde(
            tidspunkt = Instant.now(),
            avviksType = AvviksType.SLETTET
        )

        is UgyldigFeilretting -> TidspunktFraKilde(
            tidspunkt = kilde.tidspunkt ?: Instant.now(),
            avviksType = when(kilde.feilType) {
                FeilType.FeilTidspunkt -> AvviksType.TIDSPUNKT_KORRIGERT
                FeilType.Feilregistrering -> AvviksType.SLETTET
            }
        )
    }

fun feilretting(status: PeriodeTilstand, apiObj: ApiFeilretting?): Feilretting? {
    if (apiObj == null) return null
    return when (apiObj.feilType) {
        FeilType.FeilTidspunkt -> {
            when {
                apiObj.tidspunkt == null -> UgyldigFeilretting("FeilTidspunkt må ha tidspunkt", apiObj)
                apiObj.tidspunkt.isAfter(Instant.now()) -> UgyldigFeilretting("FeilTidspunkt kan ikke være i fremtiden", apiObj)
                else -> FeilTidspunkt(apiObj.melding, apiObj.tidspunkt)
            }
        }
        FeilType.Feilregistrering -> {
            when {
                status == PeriodeTilstand.STARTET -> UgyldigFeilretting("Feilregistrering kan ikke ha status STARTET", apiObj)
                apiObj.tidspunkt != null ->UgyldigFeilretting("Feilregistrering kan ikke ha tidspunkt", apiObj)
                else -> Feilregistrering(apiObj.melding)
            }
        }
    }
}

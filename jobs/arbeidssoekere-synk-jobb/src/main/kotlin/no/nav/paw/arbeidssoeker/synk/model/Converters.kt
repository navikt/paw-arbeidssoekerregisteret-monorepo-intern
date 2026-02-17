package no.nav.paw.arbeidssoeker.synk.model

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import no.nav.paw.arbeidssoeker.synk.config.DefaultVerdier
import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
private val OSLO_ZONE_ID: ZoneId = ZoneId.of("Europe/Oslo")
private val dateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT)

private fun String.asTidspunkt(): Instant {
    return LocalDateTime.parse(this, dateTimeFormatter)
        .atZone(OSLO_ZONE_ID)
        .toInstant()
}

fun ArbeidssoekerFileRow.asArbeidssoeker(
    version: String,
    defaultVerdier: DefaultVerdier
): Arbeidssoeker = Arbeidssoeker(
    version = version,
    identitetsnummer = identitetsnummer,
    periodeTilstand = defaultVerdier.periodeTilstand,
    feilretting = tidspunktFraKilde?.ifBlank { null }?.let {
        Feilretting(
            feiltype = defaultVerdier.feilrettingFeiltype,
            melding = defaultVerdier.feilrettingMelding,
            tidspunkt = it.asTidspunkt()
        )
    },
    forhaandsgodkjentAvAnsatt = defaultVerdier.forhaandsgodkjentAvAnsatt
)

fun Arbeidssoeker.asOpprettPeriodeRequest(): OpprettPeriodeRequest = OpprettPeriodeRequest(
    identitetsnummer = identitetsnummer,
    periodeTilstand = periodeTilstand.asOpprettPeriodeTilstand(),
    registreringForhaandsGodkjentAvAnsatt = forhaandsgodkjentAvAnsatt,
    feilretting = feilretting?.asOpprettPeriodeFeilretting()
)

fun PeriodeTilstand.asOpprettPeriodeTilstand(): OpprettPeriodeTilstand =
    when (this) {
        PeriodeTilstand.STARTET -> OpprettPeriodeTilstand.STARTET
        PeriodeTilstand.STOPPET -> OpprettPeriodeTilstand.STOPPET
    }

fun Feilretting.asOpprettPeriodeFeilretting(): OpprettPeriodeFeilretting =
    this.let {
        OpprettPeriodeFeilretting(
            feilType = it.feiltype.asOpprettPeriodeFeilType(),
            melding = it.melding,
            tidspunkt = it.tidspunkt
        )
    }

fun Feiltype.asOpprettPeriodeFeilType(): OpprettPeriodeFeilType =
    when (this) {
        Feiltype.FEIL_REGISTRERING -> OpprettPeriodeFeilType.Feilregistrering
        Feiltype.FEIL_TIDSPUNKT -> OpprettPeriodeFeilType.FeilTidspunkt
    }

fun ResultRow.asArbeidssoekereRow(): ArbeidssoekerDatabaseRow = ArbeidssoekerDatabaseRow(
    version = this[ArbeidssoekereSynkTable.version],
    identitetsnummer = this[ArbeidssoekereSynkTable.identitetsnummer],
    status = this[ArbeidssoekereSynkTable.status],
    inserted = this[ArbeidssoekereSynkTable.inserted],
    updated = this[ArbeidssoekereSynkTable.updated]
)

fun Instant.millisSince(): Long = Duration.between(this, Instant.now()).toMillis()

fun Int.isNotSuccess(): Boolean = !HttpStatusCode.fromValue(this).isSuccess()

package no.nav.paw.arbeidssoeker.synk.model

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import org.jetbrains.exposed.sql.ResultRow
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
private val OSLO_ZONE_ID: ZoneId = ZoneId.of("Europe/Oslo")
private val dateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT)

fun ArbeidssoekerFileRow.asArbeidssoeker(
    version: String,
    periodeTilstand: PeriodeTilstand = PeriodeTilstand.STARTET,
    forhaandsgodkjentAvAnsatt: Boolean = false
): Arbeidssoeker = Arbeidssoeker(
    version = version,
    identitetsnummer = identitetsnummer,
    periodeTilstand = periodeTilstand,
    tidspunktFraKilde = tidspunktFraKilde?.ifBlank { null }?.let {
        LocalDateTime.parse(it, dateTimeFormatter).atZone(OSLO_ZONE_ID).toInstant()
    },
    forhaandsgodkjentAvAnsatt = forhaandsgodkjentAvAnsatt
)

fun Arbeidssoeker.asOpprettPeriodeRequest(): OpprettPeriodeRequest = OpprettPeriodeRequest(
    identitetsnummer = identitetsnummer,
    periodeTilstand = asPeriodeTilstand(),
    registreringForhaandsGodkjentAvAnsatt = forhaandsgodkjentAvAnsatt,
    feilretting = asFeilretting()
)

fun Arbeidssoeker.asPeriodeTilstand(): OpprettPeriodeTilstand =
    when (periodeTilstand) {
        PeriodeTilstand.STARTET -> OpprettPeriodeTilstand.STARTET
        PeriodeTilstand.STOPPET -> OpprettPeriodeTilstand.STOPPET
    }

fun Arbeidssoeker.asFeilretting(): Feilretting? =
    tidspunktFraKilde?.let {
        Feilretting(
            feilType = FeilType.FeilTidspunkt,
            melding = "Arbeidssøker migrert fra Arena",
            tidspunkt = it
        )
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

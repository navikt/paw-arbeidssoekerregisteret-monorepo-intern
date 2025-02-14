package no.nav.paw.arbeidssoeker.synk.model

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import org.jetbrains.exposed.sql.ResultRow
import java.time.Duration
import java.time.Instant

fun Arbeidssoeker.asVersioned(version: String): VersjonertArbeidssoeker = VersjonertArbeidssoeker(
    version = version,
    identitetsnummer = identitetsnummer,
    originalStartTidspunkt = originalStartTidspunkt,
    forhaandsgodkjentAvAnsatt = forhaandsgodkjentAvAnsatt
)

fun VersjonertArbeidssoeker.asOpprettPeriodeRequest(): OpprettPeriodeRequest = OpprettPeriodeRequest(
    identitetsnummer = this.identitetsnummer,
    periodeTilstand = PeriodeTilstand.STARTET,
    registreringForhaandsGodkjentAvAnsatt = this.forhaandsgodkjentAvAnsatt,
    feilretting = Feilretting(
        feilType = FeilType.FeilTidspunkt,
        melding = "Arbeidssøker migrert fra Arena",
        tidspunkt = this.originalStartTidspunkt
    )
)

fun ResultRow.asArbeidssoekereSynkRow(): ArbeidssoekereSynkRow = ArbeidssoekereSynkRow(
    version = this[ArbeidssoekereSynkTable.version],
    identitetsnummer = this[ArbeidssoekereSynkTable.identitetsnummer],
    status = this[ArbeidssoekereSynkTable.status],
    inserted = this[ArbeidssoekereSynkTable.inserted],
    updated = this[ArbeidssoekereSynkTable.updated]
)

fun Instant.millisSince(): Long = Duration.between(this, Instant.now()).toMillis()

fun Int.isNotSuccess(): Boolean = !HttpStatusCode.fromValue(this).isSuccess()

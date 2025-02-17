package no.nav.paw.arbeidssoeker.synk.model

import java.time.Instant

data class OpprettPeriodeRequest(
    val identitetsnummer: String,
    val periodeTilstand: PeriodeTilstand,
    val registreringForhaandsGodkjentAvAnsatt: Boolean? = null,
    val feilretting: Feilretting? = null
)

enum class PeriodeTilstand(val value: String) {
    STARTET("STARTET"),
    STOPPET("STOPPET");
}

data class Feilretting(
    val feilType: FeilType,
    val melding: String? = null,
    val tidspunkt: Instant? = null
)

enum class FeilType(val value: String) {
    FeilTidspunkt("FeilTidspunkt"),
    Feilregistrering("Feilregistrering");
}

data class OpprettPeriodeErrorResponse(
    val melding: String,
    val feilKode: String,
    val aarsakTilAvvisning: AarsakTilAvvisning? = null
)

data class AarsakTilAvvisning(
    val detaljer: List<String>,
    val regler: List<AvvisningRegel>? = null
)

data class AvvisningRegel(
    val id: String? = null,
    val beskrivelse: String? = null
)

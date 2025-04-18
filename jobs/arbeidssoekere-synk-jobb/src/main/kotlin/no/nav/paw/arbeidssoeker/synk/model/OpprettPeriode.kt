package no.nav.paw.arbeidssoeker.synk.model

import java.time.Instant

data class OpprettPeriodeRequest(
    val identitetsnummer: String,
    val periodeTilstand: OpprettPeriodeTilstand,
    val registreringForhaandsGodkjentAvAnsatt: Boolean? = null,
    val feilretting: OpprettPeriodeFeilretting? = null
)

enum class OpprettPeriodeTilstand(val value: String) {
    STARTET("STARTET"),
    STOPPET("STOPPET");
}

data class OpprettPeriodeFeilretting(
    val feilType: OpprettPeriodeFeilType,
    val melding: String? = null,
    val tidspunkt: Instant? = null
)

enum class OpprettPeriodeFeilType(val value: String) {
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

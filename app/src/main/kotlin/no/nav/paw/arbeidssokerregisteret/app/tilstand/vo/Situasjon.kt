package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.Situasjon as ApiSituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottat

import java.util.*

data class Situasjon(
    val id: UUID,
    val sendtInn: Metadata,
    val utdanning: Utdanning,
    val helse: Helse,
    val arbeidserfaring: Arbeidserfaring,
    val arbeidsoekersituasjon: Arbeidsoekersituasjon
)

fun situasjon(situasjon: SituasjonMottat): Situasjon =
    Situasjon(
        id = UUID.randomUUID(),
        sendtInn = metadata(situasjon.metadata),
        utdanning = utdanning(situasjon.utdanning),
        helse = helse(situasjon.helse),
        arbeidserfaring = arbeidserfaring(situasjon.arbeidserfaring),
        arbeidsoekersituasjon = arbeidsoekersituasjon(situasjon.arbeidsoekersituasjon)
    )

fun Situasjon.api(periodeId: UUID): ApiSituasjon =
    ApiSituasjon(
        id,
        periodeId,
        sendtInn.api(),
        utdanning.api(),
        helse.api(),
        arbeidserfaring.api(),
        arbeidsoekersituasjon.api()
    )
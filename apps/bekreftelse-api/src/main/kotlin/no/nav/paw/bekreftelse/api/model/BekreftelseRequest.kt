package no.nav.paw.bekreftelse.api.model

import java.util.*

data class BekreftelseRequest(
    // Identitetsnummer må sendes med hvis det er en veileder som rapporterer
    val identitetsnummer: String? = null,
    val bekreftelseId: UUID,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)
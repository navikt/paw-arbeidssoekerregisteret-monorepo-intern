package no.nav.paw.bekreftelse.api.model

import java.util.*

data class BekreftelseRequest(
    val identitetsnummer: String,
    val bekreftelseId: UUID,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)
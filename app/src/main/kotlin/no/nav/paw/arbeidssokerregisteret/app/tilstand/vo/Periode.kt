package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import java.util.*

data class Periode(
    val id: UUID,
    val identitetsnummer: String,
    val startet: Metadata,
    val avsluttet: Metadata?

    )


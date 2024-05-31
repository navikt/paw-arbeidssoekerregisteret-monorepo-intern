package no.nav.paw.arbeidssokerregisteret.app.tilstand

import java.util.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata

data class Periode(
    val id: UUID,
    val identitetsnummer: String,
    val startet: Metadata,
    val startetVedOffset: Long = -1L,
    val avsluttet: Metadata?,
    val avsluttetVedOffset: Long? = null
)

package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeygenerator.client.Alias

data class IdMap(
    val gjeldeneIdentitetsnummer: String?,
    val arbeidsoekerId: Long,
    val recordKey: Long,
    val partisjon: Int,
    val identiteter: List<Alias>
)
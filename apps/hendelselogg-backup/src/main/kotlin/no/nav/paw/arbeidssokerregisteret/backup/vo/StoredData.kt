package no.nav.paw.arbeidssokerregisteret.backup.vo

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse

data class StoredData(
    val partition: Int,
    val offset: Long,
    val recordKey: Long,
    val arbeidssoekerId: Long,
    val data: Hendelse
)
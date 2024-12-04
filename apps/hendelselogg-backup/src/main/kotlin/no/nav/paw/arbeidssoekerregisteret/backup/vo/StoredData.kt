package no.nav.paw.arbeidssoekerregisteret.backup.vo

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse

data class StoredData(
    val partition: Int,
    val offset: Long,
    val recordKey: Long,
    val arbeidssoekerId: Long,
    val traceparent: String?,
    val data: Hendelse,
    val merged: Boolean
)
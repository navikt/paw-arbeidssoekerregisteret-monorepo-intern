package no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse

data class StoredHendelseRecord(
    val partition: Int,
    val offset: Long,
    val recordKey: Long,
    val arbeidssoekerId: Long,
    val traceparent: String?,
    val data: Hendelse,
    val merged: Boolean
)
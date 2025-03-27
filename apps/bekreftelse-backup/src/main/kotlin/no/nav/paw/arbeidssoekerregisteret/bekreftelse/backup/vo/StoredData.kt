package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse

data class StoredData(
    val partition: Int,
    val offset: Long,
    val recordKey: Long,
    val arbeidssoekerId: Long,
    val traceparent: String?,
    val data: BekreftelseHendelse,
)
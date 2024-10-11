package no.nav.paw.kafkakeygenerator.api.recordkey

import no.nav.paw.kafkakeygenerator.vo.RecordKey

data class RecordKeyLookupResponseV1(
    val key: Long
): RecordKeyResponse

fun recordKeyLookupResponseV1(
    key: RecordKey
) = RecordKeyLookupResponseV1(
    key = key.value
)
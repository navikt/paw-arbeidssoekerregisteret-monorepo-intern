package no.nav.paw.kafkakeygenerator.api.v1

import no.nav.paw.kafkakeygenerator.model.RecordKey

data class RecordKeyLookupResponseV1(
    val key: Long
): RecordKeyResponse

fun recordKeyLookupResponseV1(
    key: RecordKey
) = RecordKeyLookupResponseV1(
    key = key.value
)
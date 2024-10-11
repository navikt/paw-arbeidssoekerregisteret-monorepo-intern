package no.nav.paw.kafkakeygenerator.api.oppslag

import no.nav.paw.kafkakeygenerator.vo.RecordKey

data class RecordKeyLookupResponseV1(
    val key: Long
)

fun recordKeyLookupResponseV1(
    key: RecordKey
) = RecordKeyLookupResponseV1(
    key = key.value
)
package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.RecordKey

data class ResponseV2(
    val id: Long,
    val key: Long
)

fun responseV2(
    id: ArbeidssoekerId,
    key: RecordKey
) = ResponseV2(
    id = id.value,
    key = key.value
)
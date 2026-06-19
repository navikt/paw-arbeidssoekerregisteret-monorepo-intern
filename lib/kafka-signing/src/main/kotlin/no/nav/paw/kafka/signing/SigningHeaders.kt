package no.nav.paw.kafka.signing

import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders

const val SIGNATURE_HEADER = "x-paw-signature"
const val SIGNING_KEY_ID_HEADER = "x-paw-signing-key-id"

/**
 * Returns a new [Headers] instance with [SIGNATURE_HEADER] and [SIGNING_KEY_ID_HEADER] removed.
 * All other headers are preserved. The original [Headers] is not mutated.
 *
 * Use this in Kafka Streams topologies before forwarding records to downstream topics,
 * since signing headers are only valid for the original record.
 */
fun stripSigningHeaders(headers: Headers): Headers =
    RecordHeaders(
        headers.filter { it.key() != SIGNATURE_HEADER && it.key() != SIGNING_KEY_ID_HEADER }
    )

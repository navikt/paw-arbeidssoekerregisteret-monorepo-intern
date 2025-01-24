package no.nav.paw.kafkakeymaintenance

import no.nav.paw.kafkakeymaintenance.kafka.Topic
import java.time.Instant

const val PERIODE_CONSUMER_GROUP_VERSION = 2
const val AKTOR_CONSUMER_GROUP_VERSION = 11
val PERIODE_TOPIC = Topic("paw.arbeidssokerperioder-v1")
val AKTOR_TOPIC = Topic("pdl.aktor-v2")
val START_DATO_FOR_MERGE_PROSESSERING = Instant.parse("2025-01-24T05:00:00Z")

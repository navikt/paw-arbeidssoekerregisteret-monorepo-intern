package no.nav.paw.kafkakeymaintenance

import no.nav.paw.kafkakeymaintenance.kafka.Topic

const val PERIODE_CONSUMER_GROUP_VERSION = 2
const val AKTOR_CONSUMER_GROUP_VERSION = 6
val PERIODE_TOPIC = Topic("paw.arbeidssokerperioder-v1")
val AKTOR_TOPIC = Topic("pdl.aktor-v2")

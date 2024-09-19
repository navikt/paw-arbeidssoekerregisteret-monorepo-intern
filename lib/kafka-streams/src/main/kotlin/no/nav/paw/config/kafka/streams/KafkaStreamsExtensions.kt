package no.nav.paw.config.kafka.streams

import org.apache.kafka.streams.processor.api.Record


operator fun <K, V> Record<K, V>.component1(): K = key()
operator fun <K, V> Record<K, V>.component2(): V = value()

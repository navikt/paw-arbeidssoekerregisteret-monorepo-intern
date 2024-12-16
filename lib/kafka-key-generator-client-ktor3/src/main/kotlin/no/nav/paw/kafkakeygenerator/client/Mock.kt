package no.nav.paw.kafkakeygenerator.client

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

fun inMemoryKafkaKeysMock(): KafkaKeysClient {
    val naisClusterName = System.getenv("NAIS_CLUSTER_NAME")
    if (naisClusterName != null) {
        throw IllegalStateException("Kan ikke bruke inMemoryKafkaKeysMock i $naisClusterName")
    }
    val sekvens = AtomicLong(0)
    val map: ConcurrentMap<String, Long> = ConcurrentHashMap()
    return object: KafkaKeysClient {
        override suspend fun getIdAndKeyOrNull(identitetsnummer: String): KafkaKeysResponse {
            val id = map.computeIfAbsent(identitetsnummer) { sekvens.incrementAndGet() }
            return KafkaKeysResponse(id, id % 2)
        }

        override suspend fun getAlias(antallPartisjoner: Int, identitetsnummer: List<String>): AliasResponse =
            identitetsnummer
                .mapNotNull { id ->
                    map[id]?.let { id to it }
                }.map { (ident, arbeidssoekerId) ->
                    ident to map
                        .filterValues { it == arbeidssoekerId }
                        .map { (id, key) ->
                            Alias(
                                identitetsnummer = id,
                                arbeidsoekerId = key,
                                recordKey = key % 2,
                                partition = (key % 2).toInt() % antallPartisjoner
                            )
                        }
                }.map { (ident, aliases) ->
                    LokaleAlias(ident, aliases)
                }.let { AliasResponse(it) }
    }
}

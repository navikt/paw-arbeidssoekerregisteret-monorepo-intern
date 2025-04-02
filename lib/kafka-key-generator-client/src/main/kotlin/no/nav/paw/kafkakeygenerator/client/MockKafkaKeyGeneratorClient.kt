package no.nav.paw.kafkakeygenerator.client

import no.nav.paw.config.env.Local
import no.nav.paw.config.env.clusterNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.kafkakeygenerator.model.AliasResponse
import no.nav.paw.kafkakeygenerator.model.HentAliasResponse
import no.nav.paw.kafkakeygenerator.model.HentKeysResponse
import no.nav.paw.kafkakeygenerator.model.LokaleAliasResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

class MockKafkaKeyGeneratorClient(
    private val idSequence: AtomicLong = AtomicLong(0),
    private val idInMemoryStore: ConcurrentMap<String, Long> = ConcurrentHashMap()
) : KafkaKeyGeneratorClient {
    init {
        val runtimeEnvironment = currentRuntimeEnvironment
        if (runtimeEnvironment != Local) {
            throw IllegalStateException("Kan ikke bruke ${this.javaClass.simpleName} i ${runtimeEnvironment.clusterNameOrDefaultForLocal()}")
        }
    }

    override suspend fun hent(ident: String): HentKeysResponse? {
        return idInMemoryStore[ident]?.let { id -> HentKeysResponse(id = id, key = id.key()) }
    }

    override suspend fun hentEllerOpprett(ident: String): HentKeysResponse {
        val id = idInMemoryStore.computeIfAbsent(ident) { idSequence.incrementAndGet() }
        return HentKeysResponse(id = id, key = id.key())
    }

    override suspend fun hentAlias(antallPartisjoner: Int, identer: List<String>): HentAliasResponse {
        return identer
            .mapNotNull { ident -> idInMemoryStore[ident]?.let { id -> ident to id } }
            .map { (ident, id) ->
                ident to idInMemoryStore
                    .filterValues { it == id }
                    .map { (filterIdent, filterId) ->
                        AliasResponse(
                            identitetsnummer = filterIdent,
                            arbeidsoekerId = filterId,
                            recordKey = filterId.key(),
                            partition = filterId.partitions(antallPartisjoner)
                        )
                    }
            }.map { (ident, aliases) -> LokaleAliasResponse(ident, aliases) }
            .let { HentAliasResponse(it) }
    }

    private fun Long.key(): Long = this % 2
    private fun Long.partitions(antall: Int): Int = this.key().toInt() % antall
}
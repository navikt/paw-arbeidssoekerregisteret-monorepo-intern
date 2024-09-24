package no.nav.paw.arbeidssoekerregisteret.testdata

import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock

class KafkaKeyContext(val client: KafkaKeysClient) {
    fun getKafkaKey(id: String): KafkaKeysResponse = runBlocking {
        client.getIdAndKey(id)
    }
}

fun kafkaKeyContext(): KafkaKeyContext {
    val client = inMemoryKafkaKeysMock()
    return KafkaKeyContext(client)
}

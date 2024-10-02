package no.nav.paw.bekreftelseutgang.topology

import kotlinx.coroutines.runBlocking
import no.nav.paw.bekreftelseutgang.context.ApplicationContext
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun buildTopology(
    applicationContext: ApplicationContext
): Topology = StreamsBuilder().apply {
    //buildInternStateStore(applicationContext.applicationConfig)
    buildBekreftelseUtgangStream(applicationContext.applicationConfig)
}.build()

/*fun StreamsBuilder.buildInternStateStore(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(internStateStoreName),
                Serdes.UUID(),
                InternTilstandSerde()
            )
        )
    }
}*/

fun KafkaKeysClient.getIdAndKeyBlocking(identitetsnummer: String): KafkaKeysResponse = runBlocking {
    getIdAndKey(identitetsnummer)
}

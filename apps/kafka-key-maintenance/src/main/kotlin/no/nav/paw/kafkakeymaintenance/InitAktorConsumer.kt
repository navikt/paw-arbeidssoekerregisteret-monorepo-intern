package no.nav.paw.kafkakeymaintenance

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafka.factory.plus
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeymaintenance.kafka.*
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.LagreAktorMelding
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration

fun KafkaFactory.initAktorConsumer(
    healthIndicatorRepository: HealthIndicatorRepository,
    aktorTopic: Topic,
    applicationContext: ApplicationContext
): HwmConsumer<String, ByteArray> {
    val aktorConsumer: KafkaConsumer<String, ByteArray> = KafkaConsumer(
        baseProperties +
                mapOf(
                    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                    ConsumerConfig.GROUP_ID_CONFIG to "kafka-key-maintenance-aktor-v${applicationContext.periodeConsumerVersion}",
                    ConsumerConfig.CLIENT_ID_CONFIG to "kafka-key-maintenance-aktor-client-v${applicationContext.periodeConsumerVersion}",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 2000
                )
    )
    val reblancingListener = HwmRebalanceListener(
        contextFactory = applicationContext.aktorTxContext,
        context = applicationContext,
        consumer = aktorConsumer
    )
    transaction {
        txContext(applicationContext.aktorConsumerVersion)().initHwm(
            aktorTopic,
            aktorConsumer.partitionsFor(aktorTopic.value).count()
        )
    }
    aktorConsumer.subscribe(listOf(aktorTopic.value), reblancingListener)
    return HwmConsumer(
        name = "${aktorTopic}-consumer",
        healthIndicatorRepository = healthIndicatorRepository,
        applicationContext = applicationContext,
        contextFactory = { tx -> txContext(aktorConsumerVersion)(tx) },
        consumer = aktorConsumer,
        function = LagreAktorMelding(),
        pollTimeout = Duration.ofMillis(1000)
    )
}


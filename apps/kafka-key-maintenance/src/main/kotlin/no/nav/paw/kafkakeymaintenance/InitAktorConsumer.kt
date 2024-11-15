package no.nav.paw.kafkakeymaintenance

import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeymaintenance.kafka.*
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.lagreAktorMelding
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration

fun KafkaFactory.initAktorConsumer(
    healthIndicatorRepository: HealthIndicatorRepository,
    aktorTopic: Topic,
    applicationContext: ApplicationContext
): HwmConsumer<String, ByteArray> {

    val aktorConsumer = createConsumer(
        groupId = "kafka-key-maintenance-aktor-v${applicationContext.periodeConsumerVersion}",
        clientId = "kafka-key-maintenance-aktor-client-v${applicationContext.periodeConsumerVersion}",
        keyDeserializer = StringDeserializer::class,
        valueDeserializer = ByteArrayDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest",
        maxPollrecords = 1000
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
        function = lagreAktorMelding,
        pollTimeout = Duration.ofMillis(1000)
    )
}


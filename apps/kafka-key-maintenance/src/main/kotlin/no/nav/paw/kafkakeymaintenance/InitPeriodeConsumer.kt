package no.nav.paw.kafkakeymaintenance

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.KafkaFactory
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeymaintenance.kafka.*
import no.nav.paw.kafkakeymaintenance.perioder.lagrePeriode
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration

fun KafkaFactory.initPeriodeConsumer(
    healthIndicatorRepository: HealthIndicatorRepository,
    periodeTopic: Topic,
    applicationContext: ApplicationContext,
): HwmConsumer<Long, Periode> {

    val periodeConsumer: KafkaConsumer<Long, Periode> = createKafkaAvroValueConsumer(
        groupId = "kafka-key-maintenance-v${applicationContext.periodeConsumerVersion}",
        clientId = "kafka-key-maintenance-client-v${applicationContext.periodeConsumerVersion}",
        keyDeserializer = LongDeserializer::class,
        autoCommit = false,
        autoOffsetReset = "earliest",
        maxPollrecords = 1000
    )
    val reblancingListener = HwmRebalanceListener(
        contextFactory = applicationContext.periodeTxContext,
        context = applicationContext,
        consumer = periodeConsumer
    )
    transaction {
        txContext(applicationContext.periodeConsumerVersion)().initHwm(
            periodeTopic,
            periodeConsumer.partitionsFor(periodeTopic.value).count()
        )
    }
    periodeConsumer.subscribe(listOf(periodeTopic.value), reblancingListener)
    return HwmConsumer(
        name = "${periodeTopic}-consumer",
        healthIndicatorRepository = healthIndicatorRepository,
        applicationContext = applicationContext,
        contextFactory = { tx -> txContext(periodeConsumerVersion)(tx) },
        consumer = periodeConsumer,
        function = lagrePeriode,
        pollTimeout = Duration.ofMillis(1000)
    )
}



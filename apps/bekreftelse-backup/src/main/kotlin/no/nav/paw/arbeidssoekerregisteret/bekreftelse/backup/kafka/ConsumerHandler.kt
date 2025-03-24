package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.kafka

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.HwmRebalanceListener
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.RECORD_COUNTER
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.TransactionContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.writeBekreftelseRecord
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.writeHendelseRecord
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.writePaaVegneAvRecord
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

typealias RecordProcessor<V> = (TransactionContext, ConsumerRecord<Long, V>) -> Unit

class ConsumerHandler(
    private val context: ApplicationContext
) : AutoCloseable {
    private val logger = context.logger
    private val executor = Executors.newFixedThreadPool(3)
    private val shutdownCalled = AtomicBoolean(false)

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown signal mottatt, stopper consumers...")
            shutdownCalled.set(true)
            executor.shutdown()
        })
    }

    fun getHendelseRebalanceListener() = HwmRebalanceListener(context, context.hendelseConsumer, context.hendelseTopic)

    fun getBekreftelseRebalanceListener() = HwmRebalanceListener(context, context.bekreftelseConsumer, context.bekreftelseTopic)

    fun getPaaVegneAvRebalanceListener() = HwmRebalanceListener(context, context.paaVegneAvConsumer, context.paaVegneAvTopic)


    fun startConsumerTasks(
        pollTimeout: Duration,
    ): CompletableFuture<Void> {
        val hendelseFuture = createConsumerFuture(
            consumer = context.hendelseConsumer,
            topic = context.hendelseTopic,
            pollTimeout = pollTimeout,
            processFunction = this::processHendelseRecord
        )

        val bekreftelseFuture = createConsumerFuture(
            consumer = context.bekreftelseConsumer,
            topic = context.bekreftelseTopic,
            pollTimeout = pollTimeout,
            processFunction = this::processBekreftelseRecord
        )

        val paaVegneAvFuture = createConsumerFuture(
            consumer = context.paaVegneAvConsumer,
            topic = context.paaVegneAvTopic,
            pollTimeout = pollTimeout,
            processFunction = this::processPaaVegneAvRecord
        )

        return CompletableFuture.allOf(hendelseFuture, bekreftelseFuture, paaVegneAvFuture)
    }

    fun <V> createConsumerFuture(
        consumer: KafkaConsumer<Long, V>,
        topic: String,
        pollTimeout: Duration,
        processFunction: RecordProcessor<V>
    ): CompletableFuture<Void> {
        val rebalanceListener = when (topic) {
            context.hendelseTopic -> getHendelseRebalanceListener()
            context.bekreftelseTopic -> getBekreftelseRebalanceListener()
            context.paaVegneAvTopic -> getPaaVegneAvRebalanceListener()
            else -> throw IllegalArgumentException("Ukjent topic: $topic")
        }

        consumer.subscribe(listOf(topic), rebalanceListener)

        return CompletableFuture.runAsync(
            {
                runConsumer(
                    consumer = consumer,
                    topic = topic,
                    pollTimeout = pollTimeout,
                    processFunction = processFunction
                )
            },
            executor
        ).also {
            logger.info("Startet consumer for topic: $topic")
        }
    }

    fun <V> runConsumer(
        consumer: KafkaConsumer<Long, V>,
        topic: String,
        pollTimeout: Duration,
        processFunction: RecordProcessor<V>
    ) {
        while (!shutdownCalled.get()) {
            try {
                val records = consumer.poll(pollTimeout)
                if (!records.isEmpty) {
                    processRecords(records, topic, processFunction)
                }
            } catch (e: Exception) {
                logger.error("Error ved prosessering av records fra topic $topic", e)
                shutdownCalled.set(true)
                throw e
            }
        }
        logger.info("Consumer av topic $topic stopper")
        consumer.close()
    }

    fun <V> processRecords(
        records: ConsumerRecords<Long, V>,
        topic: String,
        processRecord: RecordProcessor<V>
    ) {
        var recordsProcessed = 0

        transaction {
            val txContext = TransactionContext(context, this)

            records.forEach { record ->
                if (txContext.updateHwm(record.partition(), record.offset(), topic)) {
                    processRecord(txContext, record)
                    recordsProcessed++
                }
            }
        }

        if (recordsProcessed > 0) {
            logger.info("$recordsProcessed prosesserte records fra topic $topic")
            context.meterRegistry.counter(RECORD_COUNTER, "topic", topic).increment(recordsProcessed.toDouble())
        }
    }

    fun processHendelseRecord(
        ctx: TransactionContext,
        record: ConsumerRecord<Long, BekreftelseHendelse>,
    ) = ctx.writeHendelseRecord(BekreftelseHendelseSerializer(), record)

    fun processBekreftelseRecord(
        ctx: TransactionContext,
        record: ConsumerRecord<Long, ByteArray>,
    ) = ctx.writeBekreftelseRecord(record)

    fun processPaaVegneAvRecord(
        ctx: TransactionContext,
        record: ConsumerRecord<Long, ByteArray>,
    ) = ctx.writePaaVegneAvRecord(record)

    override fun close() {
        logger.info("Stopper consumer handler...")
        shutdownCalled.set(true)
        executor.shutdown()
    }
}
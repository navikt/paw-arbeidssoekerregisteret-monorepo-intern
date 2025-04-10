package no.nav.paw.arbeidssoekerregisteret.backup

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.BrukerstoetteService
import no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette.initClients
import no.nav.paw.arbeidssoekerregisteret.backup.database.txContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.writeRecord
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Aarsak
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerializer
import no.nav.paw.kafka.consumer.asSequence
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration.ofMillis
import kotlin.system.exitProcess

fun main() {
    val mainLogger = LoggerFactory.getLogger("app")
    runCatching {
        val (consumer, applicationContext) = initApplication()
        with(applicationContext) {
            consumer.use {
                val hwmRebalanceListener = HwmRebalanceListener(context = this, consumer = consumer)
                meterRegistry.gauge(
                    ACTIVE_PARTITIONS_GAUGE,
                    hwmRebalanceListener
                ) { it.currentlyAssignedPartitions.size.toDouble() }
                consumer.subscribe(listOf(HENDELSE_TOPIC), hwmRebalanceListener)
                logger.info("Started subscription. Currently assigned partitions: ${consumer.assignment()}")
                val (kafkaKeysClient, oppslagAPI) = initClients(azureConfig.m2mCfg)
                val service = BrukerstoetteService(
                    oppslagAPI = oppslagAPI,
                    kafkaKeysClient = kafkaKeysClient,
                    applicationContext = applicationContext,
                    hendelseDeserializer = HendelseDeserializer()
                )
                initKtor(
                    prometheusMeterRegistry = meterRegistry,
                    binders = listOf(KafkaClientMetrics(consumer)),
                    azureConfig = azureConfig,
                    brukerstoetteService = service
                )
                runApplication(
                    hendelseSerializer = HendelseSerializer(),
                    source = consumer.asSequence(
                        stop = shutdownCalled,
                        pollTimeout = ofMillis(500),
                        closeTimeout = ofMillis(100)
                    )
                )
            }
        }
    }
        .onFailure {
            mainLogger.error("Application terminated due to an error", it)
            exitProcess(1)
        }
        .onSuccess { mainLogger.info("Application ended normally") }
}


fun ApplicationContext.runApplication(
    hendelseSerializer: HendelseSerializer,
    source: Sequence<Iterable<ConsumerRecord<Long, Hendelse>>>
) {
    val counterInclude = meterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "true")))
    val counterExclude = meterRegistry.counter(RECORD_COUNTER, listOf(Tag.of("include", "false")))

    val kalkulertAarsakCounters = Aarsak.entries.associateWith { aarsak ->
        meterRegistry.counter(
            KALKULERT_AVSLUTTET_AARSAK,
            listOf(Tag.of("kalkulert_aarsak", aarsak.name))
        )
    }
    source.forEach { records ->
        transaction {
            with(txContext(this@runApplication)()) {
                records.forEach {
                    if (updateHwm(it.partition(), it.offset())) {
                        writeRecord(hendelseSerializer, it)
                        counterInclude.increment()

                        val hendelse = it.value()
                        if (hendelse is Avsluttet) {
                            kalkulertAarsakCounters[hendelse.kalkulertAarsak]?.increment()
                        }
                    } else {
                        counterExclude.increment()
                    }
                }
            }
        }
    }
}

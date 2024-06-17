package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.database.getAllHwms
import no.nav.paw.arbeidssoekerregisteret.backup.database.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.readRecord
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class ApplicationConsumerResetTest : FreeSpec({
    "Verifiser applikasjonsflyt ved hikke i consumer" {
        with(
            ApplicationContext(
                consumerVersion = 1,
                logger = LoggerFactory.getLogger("test-logger"),
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            )
        ) {
            initDbContainer()
            val partitionCount = 3
            val testData: List<List<ConsumerRecord<Long, Hendelse>>> = listOf(
                listOf(
                    ConsumerRecord(
                        "paw.arbeidssoker-hendelseslogg-v1",
                        0,
                        0,
                        100L,
                        startet()
                    ),
                    ConsumerRecord(
                        "paw.arbeidssoker-hendelseslogg-v1",
                        0,
                        1,
                        101L,
                        avsluttet()
                    )
                ),
                listOf(
                    ConsumerRecord(
                        "paw.arbeidssoker-hendelseslogg-v1",
                        2,
                        0,
                        102L,
                        startet()
                    )
                ),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(
                    ConsumerRecord(
                        "paw.arbeidssoker-hendelseslogg-v1",
                        0,
                        0,
                        100L,
                        avsluttet()
                    )
                ),
                emptyList(),
                emptyList(),
                listOf(
                    ConsumerRecord(
                        "paw.arbeidssoker-hendelseslogg-v1",
                        0,
                        2,
                        100L,
                        avsluttet()
                    )
                )
            )
            with(HendelseSerde().serializer()) {
                transaction {
                    initHwm(partitionCount)
                }
                runApplication(testData.asSequence())
            }
            testData
                .flatten()
                .distinctBy { it.partition() to it.offset() }
                .forEach { record ->
                    val partition = record.partition()
                    val offset = record.offset()
                    val forventetHendelse = record.value()
                    val lagretHendelse = transaction {
                        with(HendelseSerde().deserializer()) {
                            readRecord(partition, offset)
                        }
                    }
                    lagretHendelse.shouldNotBeNull()
                    lagretHendelse.partition shouldBe partition
                    lagretHendelse.offset shouldBe offset
                    lagretHendelse.data shouldBe forventetHendelse
                }
        }
    }
})
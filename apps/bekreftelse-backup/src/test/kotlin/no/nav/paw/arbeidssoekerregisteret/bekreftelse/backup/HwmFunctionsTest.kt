package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.getAllHwms
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.getAllTopicHwms
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.getHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.initHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.txContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class HwmFunctionsTest : FreeSpec({
    val logger = LoggerFactory.getLogger("hwm-functions-test-logger")
    "Verifiser Hwm funksjoner" - {
        initDbContainer()
        "Vi kjører noen tester med backup versjon 1" - {
            val txCtx = txContext(
                ApplicationContext(
                    consumerVersion = 1,
                    logger = logger,
                    meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    hendelseConsumer = mockk(),
                    bekreftelseConsumer = mockk(),
                    paaVegneAvConsumer = mockk(),
                    hendelseTopic = "hendelse-test-topic",
                    bekreftelseTopic = "bekreftelse-test-topic",
                    paaVegneAvTopic = "paavegneav-test-topic",
                )
            )
            val topic = "topic1"
            "Når det ikke finnes hwm for partisjonen, skal getHwm returnere null" {
                transaction {
                    txCtx().getHwm(0, topic) shouldBe null
                }
            }
            val partitionsToInit = 6
            "Når vi initialiserer hwm for $partitionsToInit partisjoner, skal getHwm returnere -1 for partisjonene 0-${partitionsToInit - 1}" {
                transaction {
                    txCtx().initHwm(partitionsToInit, topic)
                }
                transaction {
                    for (i in 0 until partitionsToInit) {
                        txCtx().getHwm(i, topic) shouldBe -1
                    }
                }
            }
            "Vi kan oppdatere hwm for en partisjon" {
                transaction {
                    with(txCtx()) {
                        updateHwm(0, 123, topic) shouldBe true
                        updateHwm(1, 0, topic) shouldBe true
                    }
                }
                transaction {
                    with(txCtx()) {
                        getHwm(0, topic) shouldBe 123
                        getHwm(1, topic) shouldBe 0
                    }
                }
            }
            "Vi kan oppdatere hwm for en partisjon flere ganger" {
                transaction {
                    with(txCtx()) {
                        updateHwm(2, 123, topic) shouldBe true
                        updateHwm(2, 456, topic) shouldBe true
                    }
                }
                transaction {
                    txCtx().updateHwm(2, 789, topic) shouldBe true
                }
                transaction {
                    txCtx().getHwm(2, topic) shouldBe 789
                }
            }
            "Vi kan ikke oppdatere hwm for en partisjon til en lavere verdi" {
                transaction {
                    with(txCtx()) {
                        updateHwm(3, 123, topic) shouldBe true
                        updateHwm(3, 123, topic) shouldBe false
                    }
                }
                transaction {
                    with(txCtx()) {
                        updateHwm(3, 100, topic) shouldBe false
                        updateHwm(3, 0, topic) shouldBe false
                        updateHwm(3, -1, topic) shouldBe false
                    }
                }
                transaction {
                    txCtx().getHwm(3, topic) shouldBe 123
                }
            }
            "Hvis vi kjører init igjen for en partisjon, skal hwm ikke endres" {
                transaction {
                    txCtx().updateHwm(4, 2786482, topic) shouldBe true
                }
                val allHwmsBeforeNewInit = transaction { txCtx().getAllTopicHwms(topic) }
                allHwmsBeforeNewInit.find { it.partition == 4 }?.offset shouldBe 2786482
                transaction { txCtx().initHwm(partitionsToInit, topic) }
                val allHwmsAfter = transaction { txCtx().getAllTopicHwms(topic) }
                allHwmsBeforeNewInit.forEach { preNewInitHwm ->
                    allHwmsAfter.find { it.partition == preNewInitHwm.partition } shouldBe preNewInitHwm
                }
            }
        }

        "Vi kjører noen tester med backup versjon 2" - {
            val txCtx = txContext(
                ApplicationContext(
                    consumerVersion = 2,
                    logger = logger,
                    meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    hendelseConsumer = mockk(),
                    bekreftelseConsumer = mockk(),
                    paaVegneAvConsumer = mockk(),
                    hendelseTopic = "hendelse-test-topic",
                    bekreftelseTopic = "bekreftelse-test-topic",
                    paaVegneAvTopic = "paavegneav-test-topic",
                )
            )
            val topic = "topic1"
            "Vi finner ingen hwms for versjon 2" {
                transaction {
                    txCtx().getAllHwms() shouldBe emptyList()
                }
            }
            "Vi kan initialisere hwms for versjon 2" {
                transaction {
                    txCtx().initHwm(2, topic)
                }
                transaction {
                    with(txCtx()) {
                        getAllTopicHwms(topic).distinctBy { it.partition }.size shouldBe 2
                        getAllTopicHwms(topic).all { it.offset == -1L } shouldBe true
                    }
                }
            }
            "Vi kan oppdatere en hwm for versjon 2" {
                transaction {
                    txCtx().updateHwm(0, 999, topic) shouldBe true
                }
                transaction {
                    txCtx().getHwm(0, topic) shouldBe 999
                }
            }
        }
    }
})
package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.database.getAllHwms
import no.nav.paw.arbeidssoekerregisteret.backup.database.getHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class HwmFunctionsTest : FreeSpec({
    val logger = LoggerFactory.getLogger("test-logger")
    "Verify Hwm functions" - {
        initDbContainer()
        "We run som tests with backup version 1" - {
            with(ApplicationContext(
                consumerVersion = 1,
                logger = logger,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                azureConfig = loadNaisOrLocalConfiguration("azure.toml")
            )) {
                "When there is no hwm for the partition, getHwm should return null" {
                    transaction {
                        getHwm(0) shouldBe null
                    }
                }
                val partitionsToInit = 6
                "When we init hwm for $partitionsToInit partitions, getHwm should return -1 for partitions 0-${partitionsToInit - 1}" {
                    transaction {
                        initHwm(partitionsToInit)
                    }
                    transaction {
                        for (i in 0 until partitionsToInit) {
                            getHwm(i) shouldBe -1
                        }
                    }
                }
                "We can update the hwm for a partition" {
                    transaction {
                        updateHwm(0, 123) shouldBe true
                        updateHwm(1, 0) shouldBe true
                    }
                    transaction {
                        getHwm(0) shouldBe 123
                        getHwm(1) shouldBe 0
                    }
                }
                "We can update the hwm for a partition multiple times" {
                    transaction {
                        updateHwm(2, 123) shouldBe true
                        updateHwm(2, 456) shouldBe true
                    }
                    transaction {
                        updateHwm(2, 789) shouldBe true
                    }
                    transaction {
                        getHwm(2) shouldBe 789
                    }
                }
                "We can not update the hwm for a partition to a lower value" {
                    transaction {
                        updateHwm(3, 123) shouldBe true
                        updateHwm(3, 123) shouldBe false
                    }
                    transaction {
                        updateHwm(3, 100) shouldBe false
                        updateHwm(3, 0) shouldBe false
                        updateHwm(3, -1) shouldBe false
                    }
                    transaction {
                        getHwm(3) shouldBe 123
                    }
                }
                "If we run init again for a partition, the hwm should not change" {
                    transaction {
                        updateHwm(4, 2786482) shouldBe true
                    }
                    val allHwmsBeforeNewInit = transaction { getAllHwms() }
                    allHwmsBeforeNewInit.find { it.partition == 4 }?.offset shouldBe 2786482
                    transaction { initHwm(partitionsToInit) }
                    val allHwmsAfter = transaction { getAllHwms() }
                    allHwmsBeforeNewInit.forEach { preNewInitHwm ->
                        allHwmsAfter.find { it.partition == preNewInitHwm.partition } shouldBe preNewInitHwm
                    }
                }
            }
        }
        "we run some tests with backup version 2" - {
            with(ApplicationContext(
                consumerVersion = 2,
                logger = logger,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                azureConfig = loadNaisOrLocalConfiguration("azure.toml")
            )) {
                "We find no hwms for version 2" {
                    transaction {
                        getAllHwms() shouldBe emptyList()
                    }
                }
                "We can init hwms for version 2" {
                    transaction {
                        initHwm(2)
                    }
                    transaction {
                        getAllHwms().distinctBy { it.partition }.size shouldBe 2
                        getAllHwms().all { it.offset == -1L } shouldBe true
                    }
                }
                "We can update a hwm for version 2" {
                    transaction {
                        updateHwm(0, 999) shouldBe true
                    }
                    transaction {
                        getHwm(0) shouldBe 999
                    }
                }
            }
        }
    }
})
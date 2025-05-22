package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.getAllHwms
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.getHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.updateHwm
import no.nav.paw.arbeidssoekerregisteret.backup.utils.TestApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.utils.initDatabase
import org.jetbrains.exposed.sql.transactions.transaction

class HwmFunctionsTest : FreeSpec({
    with(TestApplicationContext.build()){
        "Verify Hwm functions" - {
            initDatabase()
            "We run som tests with backup version 1" - {
                val consumerVersion = applicationConfig.consumerVersion
                "When there is no hwm for the partition, getHwm should return null" {
                    transaction {
                        getHwm(consumerVersion, 0) shouldBe null
                    }
                }
                val partitionsToInit = 6
                "When we init hwm for $partitionsToInit partitions, getHwm should return -1 for partitions 0-${partitionsToInit - 1}" {
                    transaction {
                        initHwm(consumerVersion, partitionsToInit)
                    }
                    transaction {
                        for (i in 0 until partitionsToInit) {
                            getHwm(consumerVersion, i) shouldBe -1
                        }
                    }
                }
                "We can update the hwm for a partition" {
                    transaction {
                        updateHwm(consumerVersion, 0, 123) shouldBe true
                        updateHwm(consumerVersion, 1, 0) shouldBe true
                    }
                    transaction {
                        getHwm(consumerVersion, 0) shouldBe 123
                        getHwm(consumerVersion, 1) shouldBe 0
                    }
                }
                "We can update the hwm for a partition multiple times" {
                    transaction {
                        updateHwm(consumerVersion, 2, 123) shouldBe true
                        updateHwm(consumerVersion, 2, 456) shouldBe true
                    }
                    transaction {
                        updateHwm(consumerVersion, 2, 789) shouldBe true
                    }
                    transaction {
                        getHwm(consumerVersion, 2) shouldBe 789
                    }
                }
                "We can not update the hwm for a partition to a lower value" {
                    transaction {
                        updateHwm(consumerVersion, 3, 123) shouldBe true
                        updateHwm(consumerVersion, 3, 123) shouldBe false
                    }
                    transaction {
                        updateHwm(consumerVersion, 3, 100) shouldBe false
                        updateHwm(consumerVersion, 3, 0) shouldBe false
                        updateHwm(consumerVersion, 3, -1) shouldBe false
                    }
                    transaction {
                        getHwm(consumerVersion, 3) shouldBe 123
                    }
                }
                "If we run init again for a partition, the hwm should not change" {
                    transaction {
                        updateHwm(consumerVersion, 4, 2786482) shouldBe true
                    }
                    val allHwmsBeforeNewInit = transaction { getAllHwms(consumerVersion) }
                    allHwmsBeforeNewInit.find { it.partition == 4 }?.offset shouldBe 2786482
                    transaction { initHwm(consumerVersion, partitionsToInit) }
                    val allHwmsAfter = transaction { getAllHwms(consumerVersion) }
                    allHwmsBeforeNewInit.forEach { preNewInitHwm ->
                        allHwmsAfter.find { it.partition == preNewInitHwm.partition } shouldBe preNewInitHwm
                    }
                }
            }

            "we run some tests with backup version 2" - {
                val consumerVersion = 2

                "We find no hwms for version 2" {
                    transaction {
                        getAllHwms(consumerVersion) shouldBe emptyList()
                    }
                }
                "We can init hwms for version 2" {
                    transaction {
                        initHwm(consumerVersion, 2)
                    }
                    transaction {
                        getAllHwms(consumerVersion).distinctBy { it.partition }.size shouldBe 2
                        getAllHwms(consumerVersion).all { it.offset == -1L } shouldBe true
                    }
                }
                "We can update a hwm for version 2" {
                    transaction {
                        updateHwm(consumerVersion, 0, 999) shouldBe true
                    }
                    transaction {
                        getHwm(consumerVersion, 0) shouldBe 999
                    }
                }
            }
        }
    }

})
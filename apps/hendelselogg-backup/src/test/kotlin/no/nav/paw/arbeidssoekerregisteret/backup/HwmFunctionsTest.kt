package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.getAllHwms
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.getHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.updateHwm

class HwmFunctionsTest : FreeSpec({
    with(TestApplicationContext.buildWithDatabase()) {
        "Verify Hwm functions" - {
            "We run som tests with backup version 1" - {
                val consumerVersion = applicationConfig.consumerVersion
                "When there is no hwm for the partition, getHwm should return null" {
                    getHwm(consumerVersion, 0) shouldBe null
                }
                val partitionsToInit = 6
                "When we init hwm for $partitionsToInit partitions, getHwm should return -1 for partitions 0-${partitionsToInit - 1}" {
                    initHwm(consumerVersion, partitionsToInit)
                    for (i in 0 until partitionsToInit) {
                        getHwm(consumerVersion, i) shouldBe -1
                    }
                }
                "We can update the hwm for a partition" {
                    updateHwm(consumerVersion, 0, 123) shouldBe true
                    updateHwm(consumerVersion, 1, 0) shouldBe true
                    getHwm(consumerVersion, 0) shouldBe 123
                    getHwm(consumerVersion, 1) shouldBe 0
                }
                "We can update the hwm for a partition multiple times" {
                    updateHwm(consumerVersion, 2, 123) shouldBe true
                    updateHwm(consumerVersion, 2, 456) shouldBe true
                    updateHwm(consumerVersion, 2, 789) shouldBe true
                    getHwm(consumerVersion, 2) shouldBe 789
                }
                "We can not update the hwm for a partition to a lower value" {
                    updateHwm(consumerVersion, 3, 123) shouldBe true
                    updateHwm(consumerVersion, 3, 123) shouldBe false
                    updateHwm(consumerVersion, 3, 100) shouldBe false
                    updateHwm(consumerVersion, 3, 0) shouldBe false
                    updateHwm(consumerVersion, 3, -1) shouldBe false
                    getHwm(consumerVersion, 3) shouldBe 123
                }
                "If we run init again for a partition, the hwm should not change" {
                    updateHwm(consumerVersion, 4, 2786482) shouldBe true

                    val allHwmsBeforeNewInit = getAllHwms(consumerVersion)
                    allHwmsBeforeNewInit.find { it.partition == 4 }?.offset shouldBe 2786482
                    initHwm(consumerVersion, partitionsToInit)
                    val allHwmsAfter = getAllHwms(consumerVersion)
                    allHwmsBeforeNewInit.forEach { preNewInitHwm ->
                        allHwmsAfter.find { it.partition == preNewInitHwm.partition } shouldBe preNewInitHwm
                    }
                }
            }

            "we run some tests with backup version 2" - {
                val consumerVersion = 2

                "We find no hwms for version 2" {
                    getAllHwms(consumerVersion) shouldBe emptyList()
                }
                "We can init hwms for version 2" {
                    initHwm(consumerVersion, 2)
                    getAllHwms(consumerVersion).distinctBy { it.partition }.size shouldBe 2
                    getAllHwms(consumerVersion).all { it.offset == -1L } shouldBe true
                }
                "We can update a hwm for version 2" {
                    updateHwm(consumerVersion, 0, 999) shouldBe true
                    getHwm(consumerVersion, 0) shouldBe 999
                }
            }
        }
    }

})
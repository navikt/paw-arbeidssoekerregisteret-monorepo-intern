package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeymaintenance.kafka.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class HwmFunctionsTest : FreeSpec({
    val logger = LoggerFactory.getLogger("test-logger")
    "Verify Hwm functions" - {
        initDbContainer()
        val topicA = Topic("topicA")
        val topicB = Topic("topicB")
        "We run som tests with backup version 1" - {
            val txCtx = txContext(1)
            "When there is no hwm for the partition, getHwm should return null" {
                transaction {
                    txCtx().getHwm(topicA, 0) shouldBe null
                }
            }
            val partitionsToInit = 6
            "When we init hwm for $partitionsToInit partitions, getHwm should return -1 for partitions 0-${partitionsToInit - 1}" {
                transaction {
                    txCtx().initHwm(topicA, partitionsToInit)
                    txCtx().initHwm(topicB, partitionsToInit)
                }
                transaction {
                    for (i in 0 until partitionsToInit) {
                        txCtx().getHwm(topicA, i) shouldBe -1
                        txCtx().getHwm(topicB, i) shouldBe -1
                    }
                }
            }
            "We can update the hwm for a partition" {
                val time = Instant.parse("2021-09-01T12:00:00Z")
                val lastUpdated = Instant.parse("2021-09-01T12:00:10Z")
                transaction {
                    with(txCtx()) {
                        updateHwm(
                            topic = topicA,
                            partition = 0,
                            offset = 123,
                            time = time,
                            lastUpdated = lastUpdated
                        ) shouldBe true
                        updateHwm(
                            topic = topicA,
                            partition = 1,
                            offset = 0,
                            time = time - Duration.ofDays(1),
                            lastUpdated = lastUpdated - Duration.ofDays(1)
                        ) shouldBe true
                        updateHwm(
                            topic = topicA,
                            partition = 1,
                            offset = 1,
                            time = time - Duration.ofDays(2),
                            lastUpdated = lastUpdated
                        ) shouldBe true
                    }
                }
                transaction {
                    with(txCtx()) {
                        getHwm(topicA, 0) shouldBe 123
                        getHwm(topicA, 1) shouldBe 1
                        getTopicPartitionMetadata(topicA, 1) shouldBe TopicPartitionMetadata(
                            topic = topicA,
                            partition = 1,
                            offset = 1,
                            time = partitionTime(time - Duration.ofDays(1)),
                            lastUpdated = partitionLastUpdated(lastUpdated)
                        )
                    }
                }
            }
            "We can update the hwm for a partition multiple times" {
                transaction {
                    with(txCtx()) {
                        updateHwm(topicA, 2, 123, Instant.now(), Instant.now()) shouldBe true
                        updateHwm(topicA, 2, 456, Instant.now(), Instant.now()) shouldBe true
                    }
                }
                transaction {
                    txCtx().updateHwm(topicA, 2, 789, Instant.now(), Instant.now()) shouldBe true
                }
                transaction {
                    txCtx().getHwm(topicA, 2) shouldBe 789
                }
            }
            "We can not update the hwm for a partition to a lower value" {
                transaction {
                    with(txCtx()) {
                        updateHwm(topicA, 3, 123, Instant.now(), Instant.now()) shouldBe true
                        updateHwm(topicA, 3, 123, Instant.now(), Instant.now()) shouldBe false
                    }
                }
                transaction {
                    with(txCtx()) {
                        updateHwm(topicA, 3, 100, Instant.now(), Instant.now()) shouldBe false
                        updateHwm(topicA, 3, 0, Instant.now(), Instant.now()) shouldBe false
                        updateHwm(topicA, 3, -1, Instant.now(), Instant.now()) shouldBe false
                    }
                }
                transaction {
                    txCtx().getHwm(topicA, 3) shouldBe 123
                }
            }
            "If we run init again for a partition, the hwm should not change" {
                transaction {
                    txCtx().updateHwm(topicA, 4, 2786482, Instant.now(), Instant.now()) shouldBe true
                }
                val allHwmsBeforeNewInit = transaction { txCtx().getAllHwms() }
                allHwmsBeforeNewInit.find { it.topic == topicA && it.partition == 4 }?.offset shouldBe 2786482
                transaction { txCtx().initHwm(topicA, partitionsToInit) }
                val allHwmsAfter = transaction { txCtx().getAllHwms() }
                allHwmsBeforeNewInit.forEach { preNewInitHwm ->
                    allHwmsAfter.find { it.topic == preNewInitHwm.topic && it.partition == preNewInitHwm.partition } shouldBe preNewInitHwm
                }
            }
        }

        "we run some tests with backup version 2" - {
            val txCtx = txContext(2)
            "We find no hwms for version 2" {
                transaction {
                    txCtx().getAllHwms() shouldBe emptyList()
                }
            }
            "We can init hwms for version 2" {
                transaction {
                    txCtx().initHwm(topicB , 2)
                    txCtx().initHwm(topicA , 2)
                }
                transaction {
                    with(txCtx()) {
                        getAllHwms().distinctBy { it.topic to it.partition }.size shouldBe 4
                        getAllHwms().all { it.offset == -1L } shouldBe true
                    }
                }
            }
            "We can update a hwm for version 2" {
                val time = Instant.parse("2023-09-21T15:21:11Z")
                val lastUpdated = Instant.parse("2023-09-21T15:21:21Z")
                transaction {
                    txCtx().updateHwm(topicA, 0, 999, time, lastUpdated) shouldBe true
                    txCtx().updateHwm(topicA, 0, 1000, time + Duration.ofSeconds(1), lastUpdated + Duration.ofSeconds(10)) shouldBe true
                    txCtx().updateHwm(topicB, 0, 998, time - Duration.ofDays(1), lastUpdated) shouldBe true
                    txCtx().updateHwm(topicB, 0, 999, time - Duration.ofDays(1) - Duration.ofSeconds(1), lastUpdated) shouldBe true
                }
                transaction {
                    txCtx().getTopicPartitionMetadata(topicA, 0) shouldBe TopicPartitionMetadata(
                        topic = topicA,
                        partition = 0,
                        offset = 1000,
                        time = partitionTime(time + Duration.ofSeconds(1)),
                        lastUpdated = partitionLastUpdated(lastUpdated + Duration.ofSeconds(10))
                    )
                    txCtx().getTopicPartitionMetadata(topicB, 0) shouldBe TopicPartitionMetadata(
                        topic = topicB,
                        partition = 0,
                        offset = 999,
                        time = partitionTime(time - Duration.ofDays(1)),
                        lastUpdated = partitionLastUpdated(lastUpdated)
                    )
                }
            }
        }
    }
})
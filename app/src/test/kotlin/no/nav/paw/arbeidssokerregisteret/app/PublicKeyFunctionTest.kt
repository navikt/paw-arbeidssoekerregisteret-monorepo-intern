package no.nav.paw.arbeidssokerregisteret.app

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.processor.StreamPartitioner
import org.apache.kafka.streams.processor.internals.DefaultStreamPartitioner
import org.junit.jupiter.api.Assertions.*

class PublicKeyFunctionTest : FreeSpec({
    "publicTopicKeyFunction" - {
        "nøkkelen må aldri endres da dette krever replay av eventlog til nye topics" {
            val expectedModuloValue = 14_000
            PUBLIC_KEY_MODULO_VALUE shouldBe expectedModuloValue
            publicTopicKeyFunction(0) shouldBe 0L
            publicTopicKeyFunction(expectedModuloValue.toLong()) shouldBe 0L
            publicTopicKeyFunction(expectedModuloValue.toLong() + 1) shouldBe 1L
            (0 until expectedModuloValue).forEach { key ->
                publicTopicKeyFunction(key.toLong()) shouldBe key.toLong()
            }
        }
        "må gi god partisjonsdistrubusjon med default kafka oppsett" {
            val defaultPartitioner = DefaultStreamPartitioner<Long, Any>(Serdes.Long().serializer())
            val numberOfPartitions = 6
            fun getPartition(key: Long): Int = defaultPartitioner.partition(
                "topic", key, "Just some value", numberOfPartitions
            )

            val distribution = generateSequence(0) { it + 1 }
                .take(10_000_000)
                .map(Int::toLong)
                .map(::publicTopicKeyFunction)
                .map(::getPartition)
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedBy { it.second }
            println(distribution)
            distribution.size shouldBe numberOfPartitions
            val minCount = distribution.minBy { it.second }.second
            val maxCount = distribution.maxBy { it.second }.second
            val ratio = maxCount.toDouble() / minCount
            println("Største partisjon[$maxCount keys] har $ratio ganger så mange keys som minste partisjon[$minCount keys]")
            assertTrue(ratio < 1.1)
        }
        "må ikke gi for stor andel unike nøkler i forhold til input" {
            val uniqueInputKeys = 10_000_000
            val uniqueOutputKeys = generateSequence(0) { it + 1 }
                .take(uniqueInputKeys)
                .map(Int::toLong)
                .map(::publicTopicKeyFunction)
                .distinct()
                .count()
            println("Unike input keys:  $uniqueInputKeys")
            println("Unike output keys: $uniqueOutputKeys")
            assertTrue(uniqueOutputKeys < uniqueInputKeys * 0.1)
        }
    }
})
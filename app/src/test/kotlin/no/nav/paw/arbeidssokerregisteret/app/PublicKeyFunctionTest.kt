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
            val expectedModuloValue = 7_500
            PUBLIC_KEY_MODULO_VALUE shouldBe expectedModuloValue
            publicTopicKeyFunction(0) shouldBe ("internal_key_0".hashCode().toLong() % expectedModuloValue)
            publicTopicKeyFunction(expectedModuloValue.toLong()) shouldBe ("internal_key_7500".hashCode().toLong() % expectedModuloValue)
            publicTopicKeyFunction(expectedModuloValue.toLong() + 1) shouldBe ("internal_key_7501".hashCode().toLong() % expectedModuloValue)
            (0 until expectedModuloValue).forEach { key ->
                publicTopicKeyFunction(key.toLong()) shouldBe ("internal_key_$key".hashCode().toLong() % expectedModuloValue)
            }
        }
        "må gi god partisjonsdistrubusjon med default kafka oppsett" - {

            val defaultPartitioner = DefaultStreamPartitioner<Long, Any>(Serdes.Long().serializer())
            val numberOfPartitions = 6
            fun getPartition(key: Long): Int = defaultPartitioner.partition(
                "topic", key, "Just some value", numberOfPartitions
            )
            listOf(
                500_000,
                1_000_000,
                2_000_000,
                10_000_000
            ).forEach {numberOfUniqueKeys ->
                "for $numberOfUniqueKeys unike nøkler" {
                    val distribution = generateSequence(0) { it + 1 }
                        .take(numberOfUniqueKeys)
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
            }
        }
        "må ikke gi for stor andel unike nøkler i forhold til input" {
            val uniqueInputKeys = 1_000_000
            val uniqueOutputKeys = generateSequence(0) { it + 1 }
                .take(uniqueInputKeys)
                .map(Int::toLong)
                .map(::publicTopicKeyFunction)
                .distinct()
                .count()
            println("Unike input keys:  $uniqueInputKeys")
            println("Unike output keys: $uniqueOutputKeys")
            println("Personer per nøkkel: ${uniqueInputKeys / uniqueOutputKeys}")
            assertTrue(uniqueOutputKeys < uniqueInputKeys * 0.1)
        }
    }
})
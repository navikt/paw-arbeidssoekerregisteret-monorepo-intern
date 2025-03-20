package no.nav.paw.collection

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.collections.max
import no.nav.paw.collections.pawNonEmptyListOf
import no.nav.paw.collections.toPawNonEmptyListOrNull

class PawNonEmptyListTest: FreeSpec({
    "PawNonEmptyList" - {
        "should be able to create a non empty list" {
            val nonEmptyList = pawNonEmptyListOf(1, listOf(2, 3, 4))
            nonEmptyList.first shouldBe 1
            nonEmptyList.tail shouldContainExactly listOf(2, 3, 4)
        }
        "should be able to map over the list" {
            val nonEmptyList = pawNonEmptyListOf(1, listOf(2, 3, 4))
            val mappedList = nonEmptyList.map { it + 10 }
            mappedList.first shouldBe 11
            mappedList.tail shouldContainExactly listOf(12, 13, 14)
        }
        "should be able to flatMap over the list" {
            val nonEmptyList = pawNonEmptyListOf(10, listOf(20, 30, 40))
            val flatMappedList = nonEmptyList.flatMap { pawNonEmptyListOf(it + 1, listOf(it + 2, it + 3)) }
            flatMappedList.first shouldBe 11
            flatMappedList.tail shouldContainExactly listOf(12, 13, 21, 22, 23, 31, 32, 33, 41, 42, 43)
        }
        "Create an instance from a non empty List" {
            val nonEmptyList = listOf(1, 2, 3, 4).toPawNonEmptyListOrNull().shouldNotBeNull()
            nonEmptyList.first shouldBe 1
            nonEmptyList.tail shouldContainExactly listOf(2, 3, 4)
        }
        "Create an instance from an empty List" {
            val nonEmptyList = emptyList<Int>().toPawNonEmptyListOrNull()
            nonEmptyList shouldBe null
        }
        "MaxBy should return the max value" {
            val nonEmptyList = pawNonEmptyListOf(1, listOf(2, 3, 4, 5, 3, 2, 1))
            nonEmptyList.maxBy { it } shouldBe 5
        }
        "Max should return the max value" {
            val nonEmptyList = pawNonEmptyListOf(1, listOf(2, 3, 4, 5, 3, 2, 1))
            nonEmptyList.max() shouldBe 5
        }
        "MinBy should return the min value" {
            val nonEmptyList = pawNonEmptyListOf(1, listOf(2, 3, 4, 5, 3, -2, 1))
            nonEmptyList.minBy { it } shouldBe -2
        }
    }
})
package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class DiffKtTest: FreeSpec({
    "Diff med to like lister gir ingen endringer" {
        val a = listOf(
            identRad(1, "123", 1, true),
            identRad(1, "456", 2, true)
        )
        val b = listOf(
            identRad(1, "123", 1, true),
            identRad(1, "456", 2, true)
        )
        val diff = diffAtilB(a, b)
        diff.slettet shouldBe emptyList()
        diff.opprettet shouldBe emptyList()
    }

    "Diff med endret 'gjeldende' gir to 'slettet' og to 'opprettet'" {
        val a = listOf(
            identRad(1, "123", 1, true),
            identRad(1, "456", 2, false)
        )
        val b = listOf(
            identRad(1, "123", 1, false),
            identRad(1, "456", 2, true)
        )
        val diff = diffAtilB(a, b)
        diff.slettet shouldBe listOf(
            identRad(1, "123", 1, true),
            identRad(1, "456", 2, false)
        )
        diff.opprettet shouldBe listOf(
            identRad(1, "123", 1, false),
            identRad(1, "456", 2, true)
        )
    }

    "Diff med ny identitet som også er gjeldene gir en slettet og to opprettet" {
        val a = listOf(
            identRad(1, "123", 1, true),
        )
        val b = listOf(
            identRad(1, "123", 1, false),
            identRad(1, "456", 2, true),
        )
        val diff = diffAtilB(a, b)
        diff.slettet shouldBe listOf(
            identRad(1, "123", 1, true)
        )
        diff.opprettet shouldBe listOf(
            identRad(1, "123", 1, false),
            identRad(1, "456", 2, true)
        )
    }

    "Diff med ny identiet som ikke er gjeldene git ingen slettet og en opprettet" {
        val a = listOf(
            identRad(1, "123", 1, true),
        )
        val b = listOf(
            identRad(1, "123", 1, true),
            identRad(1, "456", 2, false),
        )
        val diff = diffAtilB(a, b)
        diff.slettet shouldBe emptyList()
        diff.opprettet shouldBe  listOf(
            identRad(1, "456", 2, false)
        )
    }

    "Diff med en identitet som er slettet gir en slettet og ingen opprettet" {
        val a = listOf(
            identRad(1, "123", 1, false),
            identRad(1, "456", 2, true),
        )
        val b = listOf(
            identRad(1, "123", 1, false),
        )
        val diff = diffAtilB(a, b)
        diff.slettet shouldBe listOf(
            identRad(1, "456", 2, true)
        )
        diff.opprettet shouldBe emptyList()
    }
})
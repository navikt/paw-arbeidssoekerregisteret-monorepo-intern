package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.kafkakeymaintenance.aktor
import no.nav.paw.kafkakeymaintenance.initDbContainer
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import no.nav.person.pdl.aktor.v2.Type
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

val ALL_ZEROES_TRACEPARENT = "00-00000000000000000000000000000000-0000000000000000-00".toByteArray()

class DataTableFunctionsKtTest : FreeSpec({
    "Verifiser operasjoner mot Data tabellen".config(enabled = false) - {
        initDbContainer("dataTest")
        "Vi kan skrive, oppdatere og lese data" - {
            val tcxFactory = txContext(1)
            val bytes = aktor(Triple(Type.FOLKEREGISTERIDENT, true, "12345678901")).toByteBuffer().array()
            val traceparant = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01".toByteArray()
            val timestamp = Instant.now()
            val key = UUID.randomUUID().toString()
            "Vi kan skrive data uten feil" {
                transaction {
                    tcxFactory().insertOrUpdate(
                        key = key,
                        timestamp = timestamp,
                        traceparent = traceparant,
                        data = bytes
                    )
                }
            }
            "Vi kan sjekke at vi har raden" {
                transaction {
                    tcxFactory().hasId(key) shouldBe true
                }
            }
            "Vi kan lese raden vi skrev" {
                transaction {
                    val data = tcxFactory().getBatch(1, timestamp + Duration.ofSeconds(10))
                    data.size shouldBe 1
                    data[0].id shouldBe key
                    data[0].data shouldBe bytes
                    data[0].time shouldBe timestamp.truncatedTo(ChronoUnit.MICROS)
                    data[0].traceparant shouldBe traceparant
                }
            }
            val oppdatertBytes = "Dette er en oppdaterte bytes".toByteArray()
            val oppdatertTraceparant = "Dette er en oppdatert traceparant".toByteArray()
            "Vi kan oppdatere data uten feil" {
                transaction {
                    tcxFactory().insertOrUpdate(
                        key = key,
                        timestamp = timestamp,
                        traceparent = oppdatertTraceparant,
                        data = oppdatertBytes
                    )
                }
            }
            "Vi kan lese raden vi oppdaterte" {
                transaction {
                    val data = tcxFactory().getBatch(1, timestamp + Duration.ofSeconds(10))
                    data.size shouldBe 1
                    data[0].id shouldBe key
                    data[0].data shouldBe oppdatertBytes
                    data[0].time shouldBe timestamp.truncatedTo(ChronoUnit.MICROS)
                    data[0].traceparant shouldBe oppdatertTraceparant
                }
            }
            "Vi kan slette raden vi skrev" {
                transaction {
                    tcxFactory().delete(key) shouldBe true
                }
            }
            "Vi kan ikke lese raden vi slettet" {
                transaction {
                    val data = tcxFactory().getBatch(1, timestamp - Duration.ofSeconds(10))
                    data.size shouldBe 0
                }
            }
            "Vi kan sette inn en rad uten traceparant" {
                transaction {
                    tcxFactory().insertOrUpdate(
                        key = key,
                        timestamp = timestamp,
                        traceparent = ALL_ZEROES_TRACEPARENT,
                        data = bytes
                    )
                }
            }
            "Vi kan lese raden vi satte inn uten traceparant" {
                transaction {
                    val data = tcxFactory().getBatch(1, timestamp + Duration.ofSeconds(10))
                    data.size shouldBe 1
                    data[0].id shouldBe key
                    data[0].data shouldBe bytes
                    data[0].time shouldBe timestamp.truncatedTo(ChronoUnit.MICROS)
                    data[0].traceparant shouldBe null
                }
            }
            "Vi kan sleette raden vi satte inn uten traceparant" {
                transaction {
                    tcxFactory().delete(key) shouldBe true
                }
            }
        }
    }
})
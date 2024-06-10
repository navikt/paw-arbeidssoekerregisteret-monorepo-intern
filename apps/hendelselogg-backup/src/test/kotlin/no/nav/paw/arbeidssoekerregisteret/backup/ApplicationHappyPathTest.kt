package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.getAllHwms
import no.nav.paw.arbeidssoekerregisteret.backup.database.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.readRecord
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class ApplicationHappyPathTest : FreeSpec({
    "Verifiser enkel applikasjonsflyt" {
        with(
            ApplicationContext(
                consumerVersion = 1,
                logger = LoggerFactory.getLogger("test-logger")
            )
        ) {
            initDbContainer()
            val partitionCount = 3
            val testData = hendelser()
                .take(15)
                .mapIndexed { index, hendelse ->
                    val partition = index % partitionCount
                    ConsumerRecord(
                        "paw.arbeidssoker-hendelseslogg-v1",
                        partition,
                        index.toLong(),
                        (partition + 100).toLong(),
                        hendelse
                    )
                }.chunked(5)
                .toList()
            println("Testdata: $testData")
            with(HendelseSerde().serializer()) {
                transaction {
                    initHwm(partitionCount)
                }
                runApplication(testData.asSequence())
            }
            println("HWMs: ${transaction { getAllHwms() }}")
            testData.flatten().forEach { record ->
                val partition = record.partition()
                val offset = record.offset()
                val forventetHendelse = record.value()
                val lagretHendelse = transaction {
                    with(HendelseSerde().deserializer()) {
                        readRecord(partition, offset)
                    }
                }
                lagretHendelse.shouldNotBeNull()
                lagretHendelse.partition shouldBe partition
                lagretHendelse.offset shouldBe offset
                lagretHendelse.data shouldBe forventetHendelse
            }
        }
    }
})
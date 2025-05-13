package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.database.readAllNestedRecordsForId
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.processRecords
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Kilde
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ApplicationHappyPathTest : FreeSpec({
    "Verifiser enkel applikasjonsflyt" {
        initDbContainer()
        initHwm(testApplicationContext)

        val topic = testApplicationContext.applicationConfig.hendelsesloggTopic
        val testRecords = testConsumerRecords()

        processRecords(records = testRecords, testApplicationContext)

        testRecords.forEach { record ->
            val partition = record.partition()
            val offset = record.offset()
            val forventetHendelse = record.value()

            val lagretHendelse = transaction {
                readRecord(testApplicationContext.applicationConfig.version, partition, offset)
            }
            lagretHendelse.shouldNotBeNull()
            lagretHendelse.partition shouldBe partition
            lagretHendelse.offset shouldBe offset
            lagretHendelse.data shouldBe forventetHendelse
        }

        transaction {
            val nyesteId = testRecords.records(topic).last().value().id
            val originalId = testRecords.records(topic).first().value().id
            val hendelser = readAllNestedRecordsForId(
                hendelseDeserializer = HendelseSerde().deserializer(),
                arbeidssoekerId = nyesteId,
                consumerVersion = testApplicationContext.applicationConfig.version,
                merged = true
            )
            hendelser.map { it.arbeidssoekerId }.distinct() shouldContainExactlyInAnyOrder listOf(nyesteId, originalId)
        }
    }
})

private fun testConsumerRecords(): ConsumerRecords<Long, Hendelse> {
    val topic = testApplicationContext.applicationConfig.hendelsesloggTopic
    val partitionCount = testApplicationContext.applicationConfig.partitionCount
    val records: MutableMap<TopicPartition, MutableList<ConsumerRecord<Long, Hendelse>>> =
        (0..partitionCount - 1).associate { partition ->
            val recordList = mutableListOf<ConsumerRecord<Long, Hendelse>>(
                ConsumerRecord(
                    topic,
                    partition,
                    0L,
                    partition + 100L,
                    startet((10000000000 + partition).toString(), id = 1L + partition)
                ),
                ConsumerRecord(
                    topic,
                    partition,
                    1L,
                    partition + 100L,
                    opplysninger((10000000000 + partition).toString(), id = 1L + partition)
                ),
                ConsumerRecord(
                    topic,
                    partition,
                    2L,
                    partition + 100L,
                    avsluttet((10000000000 + partition).toString(), id = 1L + partition)
                ),
            )
            TopicPartition(topic, partition) to recordList
        }.toMutableMap()

    val mergeRecord = createMergeRecord(
        originalHendelse = records.originalHendelse(),
        nyesteHendelse = records.nyesteHendelse(),
    )
    records.addRecord(mergeRecord)

    return ConsumerRecords(records)
}

private fun Map<TopicPartition, List<ConsumerRecord<Long, Hendelse>>>.originalHendelse() = entries.first().value.first()
private fun Map<TopicPartition, List<ConsumerRecord<Long, Hendelse>>>.nyesteHendelse() = entries.last().value.last()
fun MutableMap<TopicPartition, MutableList<ConsumerRecord<Long, Hendelse>>>.addRecord(mergeHendelse: ConsumerRecord<Long, Hendelse>) {
    this[TopicPartition(mergeHendelse.topic(), 5)]?.add(mergeHendelse)
}

private fun createMergeRecord(
    originalHendelse: ConsumerRecord<Long, Hendelse>,
    nyesteHendelse: ConsumerRecord<Long, Hendelse>,
) = ConsumerRecord<Long, Hendelse>(
    testApplicationContext.applicationConfig.hendelsesloggTopic,
    nyesteHendelse.partition(),
    nyesteHendelse.offset() + 1,
    nyesteHendelse.key(),
    ArbeidssoekerIdFlettetInn(
        identitetsnummer = nyesteHendelse.value().identitetsnummer,
        id = nyesteHendelse.value().id,
        hendelseId = UUID.randomUUID(),
        metadata = nyesteHendelse.value().metadata.copy(aarsak = "Merged"),
        kilde = Kilde(
            arbeidssoekerId = originalHendelse.value().id,
            identitetsnummer = setOf(
                originalHendelse.value().identitetsnummer
            )
        )
    )
)

/*
*     "Verifiser enkel applikasjonsflyt" {
        val appCtx =
            ApplicationContext(
                consumerVersion = 1,
                logger = LoggerFactory.getLogger("test-logger"),
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                azureConfig = loadNaisOrLocalConfiguration("azure.toml")
            )
        val txCtx = txContext(appCtx)
        initDbContainer()
        val partitionCount = 3
        val (idA, idB, testData) = hendelser()
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
            .let { chunks ->
                val (a, b) = chunks
                    .flatten()
                    .distinctBy { it.value().id }
                    .take(2)
                Triple(a, b, chunks)
            }
        val merge = ConsumerRecord(
            idA.topic(),
            idA.partition(),
            idA.offset() + 1000,
            (idA.partition() + 100).toLong(),
            ArbeidssoekerIdFlettetInn(
                identitetsnummer = idA.value().identitetsnummer,
                id = idA.value().id,
                hendelseId = UUID.randomUUID(),
                metadata = idA.value().metadata.copy(aarsak = "Merge"),
                kilde = Kilde(
                    arbeidssoekerId = idB.value().id,
                    identitetsnummer = setOf(
                        idB.value().identitetsnummer
                    )
                )
            ) as Hendelse
        )
        println("Testdata: $testData")
        transaction {
            txCtx().initHwm(partitionCount)
        }
        val mergeAsList = listOf(merge)
        val input = testData.plusElement(mergeAsList)
        appCtx.runApplication(HendelseSerde().serializer(), input.asSequence())
        println("HWMs: ${transaction { txCtx().getAllHwms() }}")
        testData.flatten().forEach { record ->
            val partition = record.partition()
            val offset = record.offset()
            val forventetHendelse = record.value()
            val lagretHendelse = transaction {
                txCtx().readRecord(HendelseSerde().deserializer(), partition, offset)
            }
            lagretHendelse.shouldNotBeNull()
            lagretHendelse.partition shouldBe partition
            lagretHendelse.offset shouldBe offset
            lagretHendelse.data shouldBe forventetHendelse
        }
        transaction {
            val hendelser = txCtx().readAllNestedRecordsForId(
                hendelseDeserializer = HendelseSerde().deserializer(),
                arbeidssoekerId = idA.value().id
            )
            hendelser.map { it.arbeidssoekerId }.distinct() shouldContainExactlyInAnyOrder listOf(idA.value().id, idB.value().id)
        }
    }
*/

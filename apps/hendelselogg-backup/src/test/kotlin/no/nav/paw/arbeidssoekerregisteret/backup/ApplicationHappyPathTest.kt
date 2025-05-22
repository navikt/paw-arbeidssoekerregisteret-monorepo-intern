package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordPostgresRepository.readAllNestedRecordsForId
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.utils.TestApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.utils.avsluttet
import no.nav.paw.arbeidssoekerregisteret.backup.utils.toApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.utils.opplysninger
import no.nav.paw.arbeidssoekerregisteret.backup.utils.readRecord
import no.nav.paw.arbeidssoekerregisteret.backup.utils.startet
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
        with(TestApplicationContext.buildWithDatabase().toApplicationContext()) {
            initHwm(this)
            val testRecords = testConsumerRecords(applicationConfig)

            val mergeRecord = createMergeRecord(
                originalHendelse = testRecords.originalHendelse(),
                nyesteHendelse = testRecords.nyesteHendelse(),
                applicationConfig = applicationConfig,
            )
            testRecords.addRecord(mergeRecord)

            val testConsumerRecords = ConsumerRecords(testRecords)

            hendelseloggBackup.processRecords(
                records = testConsumerRecords,
                applicationConfig.consumerVersion
            )

            testConsumerRecords.forEach { record ->
                val partition = record.partition()
                val offset = record.offset()
                val forventetHendelse = record.value()

                val lagretHendelse = transaction {
                    readRecord(applicationConfig.consumerVersion, partition, offset)
                }
                lagretHendelse.shouldNotBeNull()
                lagretHendelse.partition shouldBe partition
                lagretHendelse.offset shouldBe offset
                lagretHendelse.data shouldBe forventetHendelse
            }

            transaction {
                val topic = applicationConfig.hendelsesloggTopic
                val nyesteId = testConsumerRecords.records(topic).last().value().id
                val originalId = testConsumerRecords.records(topic).first().value().id
                val hendelser = readAllNestedRecordsForId(
                    hendelseDeserializer = HendelseSerde().deserializer(),
                    arbeidssoekerId = nyesteId,
                    consumerVersion = applicationConfig.consumerVersion,
                    merged = true
                )
                hendelser.map { it.arbeidssoekerId }.distinct() shouldContainExactlyInAnyOrder listOf(
                    nyesteId,
                    originalId
                )
            }
        }
    }


})

private fun testConsumerRecords(applicationConfig: ApplicationConfig): MutableMap<TopicPartition, MutableList<ConsumerRecord<Long, Hendelse>>> {
    val topic = applicationConfig.hendelsesloggTopic
    val partitionCount = applicationConfig.partitionCount
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

    return records
}

private fun Map<TopicPartition, List<ConsumerRecord<Long, Hendelse>>>.originalHendelse() = entries.first().value.first()
private fun Map<TopicPartition, List<ConsumerRecord<Long, Hendelse>>>.nyesteHendelse() = entries.last().value.last()

fun MutableMap<TopicPartition, MutableList<ConsumerRecord<Long, Hendelse>>>.addRecord(record: ConsumerRecord<Long, Hendelse>) {
    this[TopicPartition(record.topic(), 5)]?.add(record)
}

private fun createMergeRecord(
    originalHendelse: ConsumerRecord<Long, Hendelse>,
    nyesteHendelse: ConsumerRecord<Long, Hendelse>,
    applicationConfig: ApplicationConfig
) = ConsumerRecord<Long, Hendelse>(
    applicationConfig.hendelsesloggTopic,
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

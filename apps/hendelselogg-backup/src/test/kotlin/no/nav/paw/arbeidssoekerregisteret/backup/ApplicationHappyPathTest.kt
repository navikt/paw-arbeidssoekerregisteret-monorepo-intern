package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import no.nav.paw.arbeidssoekerregisteret.backup.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.processRecords
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Kilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.util.*

class ApplicationHappyPathTest : FreeSpec({
    "Verifiser enkel applikasjonsflyt" {
        /*
         val appCtx =
             ApplicationContextOld(
                 consumerVersion = 1,
                 logger = LoggerFactory.getLogger("test-logger"),
                 meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                 azureConfig = loadNaisOrLocalConfiguration("azure_config.toml")
             )
         val txCtx = txContext(appCtx)
         transaction {
             txCtx().initHwm(partitionCount)
         }
         println("HWMs: ${transaction { txCtx().getAllHwms() }}")

         */
        initDbContainer()

        val testHendelser = lagConsumerRecordsFra(

            hendelser = listOf(
                startet(),
                opplysninger(),
                avsluttet()
            ),
            )
        )

        val førsteHendelse = testHendelser.first()
        val andreHendelse = testHendelser[1]

        val mergeHendelse: List<ConsumerRecord<Long, Hendelse>> = genererMergeHendelse(førsteHendelse, andreHendelse)

        val records: ConsumerRecords<Long, Hendelse> =
            ConsumerRecords((testHendelser + mergeHendelse).associate { record ->
                val topicPartition = TopicPartition(record.topic(), record.partition())
                topicPartition to listOf(record)
            })

        processRecords(records = records, testApplicationContext)

        //appCtx.runApplication(HendelseSerde().serializer(), input.asSequence())
        testHendelser.forEach { record ->
            val partition = record.partition()
            val offset = record.offset()
            val forventetHendelse = record.value()
            /*
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
        */
        }
    }
})

private fun genererMergeHendelse(
    førsteHendelse: ConsumerRecord<Long, Hendelse>,
    andreHendelse: ConsumerRecord<Long, Hendelse>,
): List<ConsumerRecord<Long, Hendelse>> = listOf(
    ConsumerRecord<Long, Hendelse>(
        førsteHendelse.topic(),
        førsteHendelse.partition(),
        førsteHendelse.offset() + 1000,
        (førsteHendelse.partition() + 100).toLong(),
        ArbeidssoekerIdFlettetInn(
            identitetsnummer = førsteHendelse.value().identitetsnummer,
            id = førsteHendelse.value().id,
            hendelseId = UUID.randomUUID(),
            metadata = Metadata(aarsak = "Merge"),
            kilde = Kilde(
                arbeidssoekerId = andreHendelse.value().id,
                identitetsnummer = setOf(
                    andreHendelse.value().identitetsnummer
                )
            )
        )
    )
)

fun lagConsumerRecordsFra(
    hendelser: List<Hendelse>,
    config: ApplicationConfig = testApplicationContext.applicationConfig,
): List<ConsumerRecord<Long, Hendelse>> = hendelser.mapIndexed { index, hendelse ->
    val partition = index % config.partitionCount
    ConsumerRecord(
        config.hendelsesloggTopic,
        partition,
        index.toLong(),
        (partition + 100).toLong(),
        hendelse
    )
}.toList()

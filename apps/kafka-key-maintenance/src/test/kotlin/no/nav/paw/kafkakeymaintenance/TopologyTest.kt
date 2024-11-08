package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.kafkakeygenerator.client.Alias
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.pdlprocessor.AktorTopologyConfig
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad
import no.nav.paw.kafkakeymaintenance.perioder.statiskePerioder
import no.nav.paw.test.kafkaStreamProperties
import no.nav.paw.test.minutes
import no.nav.paw.test.opprettSerde
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import java.time.Duration
import java.time.Instant.parse
import java.util.*

class TopologyTest : FreeSpec({
    val testTime = parse("2021-12-03T12:00:00Z")

    "Test standard merge" {
        val person1Alias = LokaleAlias(
            periodePerson1.identitetsnummer, listOf(
                Alias(
                    identitetsnummer = periodePerson1.identitetsnummer,
                    arbeidsoekerId = 0L,
                    recordKey = 0L,
                    partition = 0
                )
            )
        )
        val person2Alias = LokaleAlias(
            periodePerson2.identitetsnummer, listOf(
                Alias(
                    identitetsnummer = periodePerson2.identitetsnummer,
                    arbeidsoekerId = 1L,
                    recordKey = 1L,
                    partition = 1
                )
            )
        )
        val aliasMap = mapOf(
            periodePerson1.identitetsnummer to person1Alias,
            periodePerson2.identitetsnummer to person2Alias
        )

        fun hentAlias(identitetsnummer: List<String>): List<LokaleAlias> {
            return identitetsnummer.mapNotNull { aliasMap[it] }
        }

        val topology = initTopology(
            stateStoreBuilderFactory = Stores::inMemoryKeyValueStore,
            aktorTopologyConfig = aktorTopologyConfig,
            perioder = perioder,
            hentAlias = ::hentAlias,
            aktorSerde = opprettSerde()
        )
        val testDriver = TopologyTestDriver(topology, kafkaStreamProperties, testTime)
        val aktorTopic = testDriver.createInputTopic(
            aktorTopologyConfig.aktorTopic,
            Serdes.StringSerde().serializer(),
            opprettSerde<Aktor>().serializer()
        )
        val hendelseloggTopic = testDriver.createOutputTopic(
            aktorTopologyConfig.hendelseloggTopic,
            Serdes.Long().deserializer(),
            HendelseSerde().deserializer()
        )
        aktorTopic.pipeInput(
            "p1",
            aktor(
                id(identifikasjonsnummer = periodePerson1.identitetsnummer, gjeldende = true),
                id(identifikasjonsnummer = periodePerson2.identitetsnummer, gjeldende = false)
            ),
            testTime
        )
        hendelseloggTopic.isEmpty shouldBe true
        testDriver.advanceWallClockTime(aktorTopologyConfig.supressionDelay - 1.minutes)
        hendelseloggTopic.isEmpty shouldBe true
        testDriver.advanceWallClockTime(5.minutes)
        hendelseloggTopic.isEmpty shouldBe false
        hendelseloggTopic.readKeyValuesToList() should {
            it.size shouldBe 1
            it.first() should { (key, hendelse) ->
                key shouldBe 1L
                hendelse.identitetsnummer shouldBe periodePerson2.identitetsnummer
                hendelse.shouldBeInstanceOf<IdentitetsnummerSammenslaatt>()
                hendelse.flyttetTilArbeidssoekerId shouldBe 0L
                hendelse.alleIdentitetsnummer shouldBe listOf(periodePerson2.identitetsnummer)
            }
        }

    }
})

operator fun <K, V> KeyValue<K, V>.component1(): K = key
operator fun <K, V> KeyValue<K, V>.component2(): V = value

fun aktor(
    vararg id: Triple<Type, Boolean, String>
): Aktor = Aktor(
    id.map { Identifikator(it.third, it.first, it.second) }
)

fun id(
    type: Type = Type.FOLKEREGISTERIDENT,
    gjeldende: Boolean = false,
    identifikasjonsnummer: String
): Triple<Type, Boolean, String> = Triple(type, gjeldende, identifikasjonsnummer)

val perioder = statiskePerioder(
    mapOf(
        periodePerson1.identitetsnummer to periodePerson1,
        periodePerson2.identitetsnummer to periodePerson2
    )
)

val periodePerson1
    get() = PeriodeRad(
        periodeId = UUID.randomUUID(),
        identitetsnummer = "12345678901",
        fra = parse("2021-09-01T12:00:00Z"),
        til = null
    )

val periodePerson2
    get() = PeriodeRad(
        periodeId = UUID.randomUUID(),
        identitetsnummer = "12345678902",
        fra = parse("2021-09-11T12:00:00Z"),
        til = parse("2021-09-27T12:00:00Z")
    )
val aktorTopologyConfig
    get() = AktorTopologyConfig(
        aktorTopic = "aktor_topic",
        hendelseloggTopic = "hendelselogg_topic",
        supressionDelay = Duration.ofHours(1),
        interval = Duration.ofMinutes(1),
        stateStoreName = "suppression_store"
    )

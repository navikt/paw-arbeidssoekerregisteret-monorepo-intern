package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.state.ValueAndTimestamp
import java.time.Duration
import java.time.Instant
import java.util.*

class OpplysningerKStreamTest : FreeSpec({

    with(TestContext()) {
        "Testsuite for aggregering av opplysinger om arbeidssøker" - {

            "Skal lagre opplysinger ved mottak" {
                val id = UUID.randomUUID()
                val periodeId = UUID.randomUUID()
                val ident = "01017012345"
                val key = 1L
                val opplysninger = buildOpplysninger(id, periodeId, ident)
                opplysingerTopic.pipeInput(key, opplysninger)

                stateStore.size() shouldBe 1
                val valueAndTimestamp =
                    stateStore.get(id.toString()).shouldBeInstanceOf<ValueAndTimestamp<OpplysningerOmArbeidssoeker>>()
                valueAndTimestamp shouldNotBe null
                valueAndTimestamp.value() shouldNotBe null
                valueAndTimestamp.value().id shouldBe opplysninger.id
                valueAndTimestamp.value().periodeId shouldBe opplysninger.periodeId
                valueAndTimestamp.value().sendtInnAv.utfoertAv.id shouldBe opplysninger.sendtInnAv.utfoertAv.id
            }

            "Skal slette opplysninger etter 60 minutter" {
                testDriver.advanceWallClockTime(Duration.ofMinutes(65)) // 60 min++

                stateStore.size() shouldBe 0
            }
        }
    }

}) {
    private class TestContext : CommonTestContext() {

        val sourceKeySerde: Serde<Long> = Serdes.Long()
        val sourceValueSerde: Serde<OpplysningerOmArbeidssoeker> = MockSchemaRegistryAvroSerde()
        val storeKeySerde: Serde<String> = Serdes.String()

        val testDriver = with(ApplicationContext(logger, applicationProperties)) {
            StreamsBuilder().apply {
                addStateStore(
                    Stores.timestampedKeyValueStoreBuilder(
                        Stores.inMemoryKeyValueStore(properties.kafkaStreams.opplysningerStore),
                        storeKeySerde,
                        sourceValueSerde
                    )
                )
                addOpplysningerKStream(meterRegistry)
            }.build()
        }.let { TopologyTestDriver(it, kafkaStreamProperties) }

        val stateStore: KeyValueStore<String, ValueAndTimestamp<OpplysningerOmArbeidssoeker>> = testDriver
            .getTimestampedKeyValueStore(applicationProperties.kafkaStreams.opplysningerStore)

        val opplysingerTopic: TestInputTopic<Long, OpplysningerOmArbeidssoeker> = testDriver.createInputTopic(
            applicationProperties.kafkaStreams.opplysningerTopic,
            sourceKeySerde.serializer(),
            sourceValueSerde.serializer()
        )

        fun buildOpplysninger(id: UUID, periodeId: UUID, ident: String): OpplysningerOmArbeidssoeker {
            return OpplysningerOmArbeidssoeker(
                id,
                periodeId,
                buildMetadata(ident),
                buildUtdanning(),
                buildHelse(),
                buildJobbsituasjon(),
                buildAnnet()
            )
        }

        fun buildMetadata(ident: String): Metadata {
            return Metadata(
                Instant.now(),
                buildBruker(ident),
                "test",
                "test",
                TidspunktFraKilde(Instant.now(), AvviksType.UKJENT_VERDI)
            )
        }

        fun buildBruker(ident: String): Bruker {
            return Bruker(BrukerType.SLUTTBRUKER, ident)
        }

        fun buildUtdanning(): Utdanning {
            return Utdanning("69", JaNeiVetIkke.JA, JaNeiVetIkke.JA)
        }

        fun buildHelse(): Helse {
            return Helse(JaNeiVetIkke.NEI)
        }

        fun buildJobbsituasjon(): Jobbsituasjon {
            val beskrivelse = BeskrivelseMedDetaljer(
                Beskrivelse.ANNET, mapOf(
                    "stilling" to "test",
                    "stilling_styrk08" to "test"
                )
            )
            return Jobbsituasjon(listOf(beskrivelse))
        }

        fun buildAnnet(): Annet {
            return Annet(JaNeiVetIkke.NEI)
        }
    }
}
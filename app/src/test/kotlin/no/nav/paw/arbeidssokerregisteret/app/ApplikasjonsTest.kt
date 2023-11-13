package no.nav.paw.arbeidssokerregisteret.app

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.GJELDER_FRA_DATO
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.STILLING
import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import java.time.Instant
import java.util.*


class ApplikasjonsTest : FreeSpec({
    val topology = topology(
        builder = opprettStreamsBuilder(dbNavn, tilstandSerde),
        dbNavn = dbNavn,
        innTopic = eventlogTopicNavn,
        periodeTopic = periodeTopicNavn,
        situasjonTopic = situasjonTopicNavn
    )

    val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)
    val eventlogTopic = testDriver.createInputTopic(
        eventlogTopicNavn,
        Serdes.Long().serializer(),
        hendelseSerde.serializer()
    )
    val periodeTopic = testDriver.createOutputTopic(
        periodeTopicNavn,
        Serdes.Long().deserializer(),
        periodeSerde.deserializer()
    )
    val situasjonTopic = testDriver.createOutputTopic(
        situasjonTopicNavn,
        Serdes.Long().deserializer(),
        situasjonSerde.deserializer()
    )
    val identitetnummer = "12345678901"
    val key = 5L
    "Verifiser applikasjonsflyt" - {
        val startet = Startet(
            identitetnummer,
            Metadata(
                Instant.now(),
                Bruker(BrukerType.SYSTEM, "test"),
                "unit-test",
                "tester"
            )
        )
        var periodeId: UUID? = null
        "Når vi mottar en 'startet' hendelse og ikke har noen tidligere tilstand skal vi opprette en ny periode" {
            eventlogTopic.pipeInput(key, startet)
            val periode = periodeTopic.readKeyValue()
            situasjonTopic.isEmpty shouldBe true
            verifiserPeriodeOppMotStartetOgStoppetHendelser(
                forventetKafkaKey = key,
                startet = startet,
                stoppet = null,
                mottattRecord = periode
            )
            periodeId = periode.value.id
        }

        "Når vi mottar en 'startet' hendelse for en person med en aktiv periode skal det ikke skje noe" {
            val duplikatStart = Startet(
                identitetnummer,
                Metadata(
                    Instant.now(),
                    Bruker(BrukerType.SYSTEM, "test"),
                    "unit-test",
                    "tester"
                )
            )
            eventlogTopic.pipeInput(key, duplikatStart)
            periodeTopic.isEmpty shouldBe true
            situasjonTopic.isEmpty shouldBe true
        }

        "Når vi mottar en ny situsjon for en person med en aktiv periode skal vi sende ut en ny situasjon" {
            val situsjonMottat = SituasjonMottat(
                identitetnummer,
                Metadata(
                    Instant.now(),
                    Bruker(BrukerType.SYSTEM, "test"),
                    "unit-test",
                    "tester"
                ),
                Utdanning(Utdanningsnivaa.HOYERE_UTDANNING_1_TIL_4, JaNeiVetIkke.JA, JaNeiVetIkke.JA),
                Helse(JaNeiVetIkke.JA),
                Arbeidserfaring(JaNeiVetIkke.JA),
                Arbeidsoekersituasjon(mutableListOf(Element(Beskrivelse.ER_PERMITTERT, mutableMapOf(
                    PROSENT to "100",
                    GJELDER_FRA_DATO to "2020-01-01",
                    STILLING to "Lærer",
                    STILLING_STYRK08 to "2320"
                ))))
            )
            eventlogTopic.pipeInput(key, situsjonMottat)
            periodeTopic.isEmpty shouldBe true
            val situasjon = situasjonTopic.readKeyValue()
            situasjon.key shouldBe key
            situasjon.value.periodeId shouldBe periodeId
        }

        "Når vi mottat en 'stoppet' hendelse for en person med en aktiv periode skal vi avslutte perioden" {
            val stoppet = Stoppet(
                identitetnummer,
                Metadata(
                    Instant.now(),
                    Bruker(BrukerType.SYSTEM, "test"),
                    "unit-test",
                    "tester"
                )
            )
            eventlogTopic.pipeInput(key, stoppet)
            situasjonTopic.isEmpty shouldBe true
            periodeTopic.isEmpty shouldBe false
            val avsluttetPeriode = periodeTopic.readKeyValue()
            verifiserPeriodeOppMotStartetOgStoppetHendelser(
                forventetKafkaKey = key,
                startet = startet,
                stoppet = stoppet,
                mottattRecord = avsluttetPeriode
            )
        }
    }
})

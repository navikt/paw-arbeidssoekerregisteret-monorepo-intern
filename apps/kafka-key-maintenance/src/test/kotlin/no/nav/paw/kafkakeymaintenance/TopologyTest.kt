package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.intern.v1.HarIdentitetsnummer
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
    val testTime = parse("2021-12-12T12:00:00Z")
    with(initTopologyTestContext(testTime)) {
        "Test standard merge" {
            val person1 = "12345678901"
            val person2 = "12345678902"
            addAlias(person1, alias(person1, 0L))
            addAlias(person2, alias(person2, 1L))
            addPeriode(testPeriode(identitetsnummer = person1, fra = "2021-12-04T12:00:00Z"))
            addPeriode(testPeriode(identitetsnummer = person2, fra = "2021-12-05T12:00:00Z", til = "2021-12-08T12:00:00Z"))

            aktorTopic.pipeInput(
                "p1",
                aktor(//person1 og person2 er samme person i følge PDL, gjeldene identitetsnummer er person1
                    id(identifikasjonsnummer = person1, gjeldende = true),
                    id(identifikasjonsnummer = person2, gjeldende = false)
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
                    hendelse.identitetsnummer shouldBe person2
                    hendelse.shouldBeInstanceOf<IdentitetsnummerSammenslaatt>()
                    hendelse.flyttetTilArbeidssoekerId shouldBe 0L
                    hendelse.alleIdentitetsnummer shouldBe listOf(person2)
                }
            }
        }

    }
})




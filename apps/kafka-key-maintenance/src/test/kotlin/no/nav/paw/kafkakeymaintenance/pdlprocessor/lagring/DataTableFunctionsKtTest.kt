package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.kafkakeymaintenance.aktor
import no.nav.paw.kafkakeymaintenance.initDbContainer
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.tilInternType
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.record.TimestampType
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class DataTableFunctionsKtTest : FreeSpec({
    "Database relaterte tester".config(enabled = true) - {
        initDbContainer("dataTest")
        "Verifiser operasjoner mot Data tabellen".config(enabled = true) - {
            "Vi kan skrive, oppdatere og lese data" - {
                val tcxFactory = txContext(1)
                val aktor = aktor(Triple(Type.FOLKEREGISTERIDENT, true, "12345678901"))
                val traceparent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
                val timestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)
                val key = "AB123456789012345678"
                val tidspunktFraKilde = timestamp - Duration.ofMinutes(10)
                var _personId: Long? = null
                "Vi kan skrive data uten feil" {
                    transaction {
                        tcxFactory().settInEllerOppdatere(
                            recordKey = key,
                            tidspunktFraKilde = tidspunktFraKilde,
                            tidspunkt = timestamp,
                            aktor = aktor,
                            traceparent = traceparent
                        )
                    } should { resultat ->
                        _personId = resultat.person.personId
                        resultat.person.traceparant shouldBe traceparent
                        resultat.person.recordKey shouldBe key
                        resultat.person.sistEndret shouldBe timestamp
                        resultat.person.tidspunktFraKilde shouldBe tidspunktFraKilde
                        resultat.forrigeTilstand.size shouldBe 0
                        resultat.nyTilstand.size shouldBe 1
                        resultat.nyTilstand.first().ident shouldBe aktor.identifikatorer.first().idnummer
                        resultat.nyTilstand.first().gjeldende shouldBe aktor.identifikatorer.first().gjeldende
                    }
                }
                val personId = _personId ?: fail("Test feilet, personId ikke tilgjenglig")
                "Vi har har personen vi skrev" {
                    transaction {
                        tcxFactory().hentPerson(key).shouldNotBeNull() should { person ->
                            person.tidspunktFraKilde shouldBe tidspunktFraKilde
                            person.traceparant shouldBe traceparent
                            person.recordKey shouldBe key
                            person.sistEndret shouldBe timestamp
                        }
                    }
                }
                "Vi har ident'en vi skrev" {
                    transaction {
                        tcxFactory().hentIdenter(personId)
                    } should { identer ->
                        identer.size shouldBe 1
                        identer[0].ident shouldBe aktor.identifikatorer[0].idnummer
                        identer[0].gjeldende shouldBe aktor.identifikatorer[0].gjeldende
                        identer[0].identType shouldBe aktor.identifikatorer[0].type.tilInternType()
                    }
                }
                "Vi finner raden i listen over ikke prosesserte personer" {
                    transaction {
                        val data = tcxFactory().hentIkkeProsessertePersoner(1, timestamp + Duration.ofSeconds(10))
                        data.size shouldBe 1
                        data[0].traceparant shouldBe traceparent
                        data[0].recordKey shouldBe key
                        data[0].sistEndret shouldBe timestamp
                        data[0].tidspunktFraKilde shouldBe tidspunktFraKilde
                    }
                }
                val oppdatertTraceparant = "Dette er en oppdatert traceparant"
                val oppdatertAktor = Aktor(
                    aktor.identifikatorer + Identifikator(
                        "01010112345",
                        Type.FOLKEREGISTERIDENT,
                        false
                    )
                )
                "Vi kan oppdatere data uten feil" {
                    transaction {
                        tcxFactory().settInEllerOppdatere(
                            recordKey = key,
                            tidspunktFraKilde = tidspunktFraKilde + Duration.ofMinutes(30),
                            tidspunkt = timestamp + Duration.ofMinutes(30),
                            traceparent = oppdatertTraceparant,
                            aktor = oppdatertAktor
                        )
                    }
                }
                "Vi kan lese raden vi oppdaterte" {
                    transaction {
                        val data = tcxFactory().hentIkkeProsessertePersoner(1, timestamp + Duration.ofMinutes(60))
                        data.size shouldBe 1
                        data[0].recordKey shouldBe key
                        data[0].tidspunktFraKilde shouldBe tidspunktFraKilde + Duration.ofMinutes(30)
                        data[0].traceparant shouldBe oppdatertTraceparant
                        val lagredeIdenter = tcxFactory().hentIdenter(data[0].personId)
                        lagredeIdenter.size shouldBe oppdatertAktor.identifikatorer.size
                        oppdatertAktor.identifikatorer.forEach { ident ->
                            val identType = ident.type.tilInternType()
                            val identNummer = ident.idnummer
                            val gjeldende = ident.gjeldende
                            lagredeIdenter.filter {
                                it.ident == identNummer && it.identType == identType && it.gjeldende == gjeldende
                            }.size shouldBe 1
                        }
                        data[0].personId
                    }
                }
                "Vi kan markerer en person som prosessert" {
                    val pid = personId.shouldNotBeNull()
                    transaction {
                        tcxFactory().mergeProsessert(pid) shouldBe true
                    }
                }
                "Vi kan ikke lese personen vi slettet" {
                    transaction {
                        val data = tcxFactory().hentIkkeProsessertePersoner(1, timestamp - Duration.ofSeconds(10))
                        data.size shouldBe 0
                    }
                }
            }
        }
    }

    "Verifiser fullstendig behandling av innkommende Aktor melding".config(enabled = true) - {
        val lagreAktorMeldinger = LagreAktorMelding()
        val tcxFactory = txContext(2)
        val aktorId = "AC123456789012345678"
        val tidFraKilde = Instant.now().minus(Duration.ofDays(2)).minus(Duration.ofMinutes(10)).truncatedTo(ChronoUnit.MILLIS)
        val foersteAktorMelding = aktor(
            Triple(Type.FOLKEREGISTERIDENT, true, "12345678901"),
            Triple(Type.FOLKEREGISTERIDENT, false, "01010112345"),
            Triple(Type.AKTORID, false, aktorId)
        )
        "Når vi har prosessert en melding, kan vi hente den ut igjen" {
            transaction {
                lagreAktorMeldinger.process(tcxFactory(), consumerRecord(aktorId, tidFraKilde, foersteAktorMelding))
            }
            transaction {
                tcxFactory().hentIkkeProsessertePersoner(1, Instant.now() + Duration.ofDays(10))
            } should { personer ->
                personer.size shouldBe 1
                personer[0].recordKey shouldBe aktorId
                personer[0].tidspunktFraKilde shouldBe tidFraKilde
            }
        }
    }
})

fun consumerRecord(
    key: String,
    timestamp: Instant,
    aktor: Aktor,
): ConsumerRecord<String, Aktor> = mockk {
    every { key() } returns key
    every { timestamp() } returns timestamp.toEpochMilli()
    every { timestampType() } returns TimestampType.NO_TIMESTAMP_TYPE
    every { value() } returns aktor
}
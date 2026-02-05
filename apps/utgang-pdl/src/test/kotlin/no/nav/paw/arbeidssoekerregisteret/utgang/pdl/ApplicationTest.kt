package no.nav.paw.arbeidssoekerregisteret.utgang.pdl

import io.kotest.assertions.AssertionErrorBuilder.Companion.fail
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import org.apache.kafka.streams.TestOutputTopic
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata as MetadataIntern
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker as BrukerIntern
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType as BrukerTypeIntern
import java.time.Duration
import java.time.Instant
import java.util.*

class ApplicationTest : FreeSpec({
    val periodeId = UUID.randomUUID()
    val identitetsnummer = "12345678901"

    "Sender Avsluttet hendelse for person med forenkletStatus 'doedIFolkeregisteret' i PDL og ingen opplysninger fra hendelser" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("doedIFolkeregisteret")
                    ),
                    "ok",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(1234L,
                Startet(
                    periodeId,
                    1234L,
                    identitetsnummer,
                    MetadataIntern(
                        Instant.now(),
                        BrukerIntern(
                            BrukerTypeIntern.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        ""
                    ),
                    emptySet()
                )
            )
            periodeTopic.pipeInput(
                1234L,
                Periode(
                    periodeId,
                    identitetsnummer,
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(2))
            hendelseloggOutputTopic.isEmpty shouldBe false
            hendelseloggOutputTopic.readValue()
        }
    }

    "Sender ikke Avsluttet hendelse for person med forenkletStatus 'bosattEtterFolkeregisterloven' i PDL og ingen opplysninger fra hendelser" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("bosattEtterFolkeregisterloven")
                    ),
                    "ok",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(
                1234L,
                Startet(
                    periodeId,
                    1234,
                    identitetsnummer,
                    MetadataIntern(
                        Instant.now(),
                        BrukerIntern(
                            BrukerTypeIntern.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        ""
                    ),
                    emptySet()
                )
            )
            periodeTopic.pipeInput(
                1234L,
                Periode(
                    periodeId,
                    identitetsnummer,
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(2))
            verifyEmptyTopic(hendelseloggOutputTopic)
        }
    }

    "Sender ikke Avsluttet hendelse om hentPersonBolk status er 'bad_request' eller 'not_found' i PDL" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("doedIFolkeregisteret", "dNummer")
                    ),
                    "not_found",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            periodeTopic.pipeInput(
                1234L,
                Periode(
                    periodeId,
                    identitetsnummer,
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )
            periodeTopic.pipeInput(
                1234L,
                Periode(
                    periodeId,
                    identitetsnummer,
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(2))
            verifyEmptyTopic(hendelseloggOutputTopic)
        }
    }

    "Sender ikke Avsluttet hendelse for person med forenkletStatus 'ikkeBosatt' i PDL og opplysninger FORHAANDSGODKJENT_AV_ANSATT og IKKE_BOSATT fra hendelser" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("ikkeBosatt", "dNummer")
                    ),
                    "ok",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(
                1234L,
                Startet(
                    periodeId,
                    1L,
                    identitetsnummer,
                    MetadataIntern(
                        Instant.now(),
                        BrukerIntern(
                            BrukerTypeIntern.VEILEDER,
                            "1234",
                            null
                        ),
                        "",
                        ""
                    ),
                    setOf(
                        Opplysning.DNUMMER,
                        Opplysning.FORHAANDSGODKJENT_AV_ANSATT,
                        Opplysning.IKKE_BOSATT
                    )
                )
            )
            periodeTopic.pipeInput(
                1234L,
                Periode(
                    periodeId,
                    identitetsnummer,
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(2))
            verifyEmptyTopic(hendelseloggOutputTopic)
        }
    }

    "Sender Avsluttet hendelse for person med forenkletStatus 'ikkeBosatt' i PDL og opplysninger FORHAANDSGODKJENT_AV_ANSATT, ER_UNDER_18_AAR og BOSATT_ETTER_FREG_LOVEN fra hendelser" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("ikkeBosatt")
                    ),
                    "ok",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(
                1234L,
                Startet(
                    periodeId,
                    1234L,
                    identitetsnummer,
                    MetadataIntern(
                        Instant.now(),
                        BrukerIntern(
                            BrukerTypeIntern.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        ""
                    ),
                    setOf(
                        Opplysning.FORHAANDSGODKJENT_AV_ANSATT,
                        Opplysning.ER_UNDER_18_AAR,
                        Opplysning.BOSATT_ETTER_FREG_LOVEN
                    )
                )
            )
            periodeTopic.pipeInput(
                1234L,
                Periode(
                    periodeId,
                    identitetsnummer,
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(2))
            hendelseloggOutputTopic.isEmpty shouldBe false
            hendelseloggOutputTopic.readValue()
        }
    }

    "Sender Avsluttet hendelse for person om forenkletStatus er 'opphoert' i PDL og opplysninger er 'FORHAANDSGODKJENT_AV_ANSATT' og 'DNUMMER'" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("opphoert")
                    ),
                    "ok",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(
                1234L,
                Startet(
                    periodeId,
                    1234L,
                    identitetsnummer,
                    MetadataIntern(
                        Instant.now(),
                        BrukerIntern(
                            BrukerTypeIntern.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        ""
                    ),
                    setOf(
                        Opplysning.DNUMMER,
                        Opplysning.FORHAANDSGODKJENT_AV_ANSATT,
                    )
                )
            )
            periodeTopic.pipeInput(
                1234L,
                Periode(
                    periodeId,
                    identitetsnummer,
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(2))
            hendelseloggOutputTopic.isEmpty shouldBe false
            hendelseloggOutputTopic.readValue()
        }
    }

    "Fjerner hendelse fra hendelse state store hvis ingen tilhørende periode har kommet innen 30 dager" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("opphoert")
                    ),
                    "ok",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(
                1234L,
                Startet(
                    periodeId,
                    1234L,
                    identitetsnummer,
                    MetadataIntern(
                        Instant.now(),
                        BrukerIntern(
                            BrukerTypeIntern.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        ""
                    ),
                    setOf(
                        Opplysning.DNUMMER,
                        Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                    )
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(31))
            hendelseKeyValueStore.get(periodeId) shouldBe null
        }
    }

    "Fjerner ikke hendelse fra hendelse state store hvis ingen tilhørende periode har kommet innen 29 dager" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("opphoert")
                    ),
                    "ok",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(
                1234L,
                Startet(
                    periodeId,
                    1234L,
                    identitetsnummer,
                    MetadataIntern(
                        Instant.now(),
                        BrukerIntern(
                            BrukerTypeIntern.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        ""
                    ),
                    setOf(
                        Opplysning.DNUMMER,
                        Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                    )
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(29))
            hendelseKeyValueStore.get(periodeId) shouldNotBe null
        }
    }

    "Fjerner ikke hendelse fra hendelse state store hvis tilhørende periode har kommet innen 30 dager" {
        with(
            testScope(
                generatePdlMockResponse(
                    identitetsnummer,
                    getPerson(
                        folkeregisterpersonstatus = getListOfFolkeregisterpersonstatus("bosattEtterFolkeregisterloven")
                    ),
                    "ok",
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(
                1234L,
                Startet(
                    periodeId,
                    1234L,
                    identitetsnummer,
                    MetadataIntern(
                        Instant.now(),
                        BrukerIntern(
                            BrukerTypeIntern.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        ""
                    ),
                    emptySet()
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(10))
            periodeTopic.pipeInput(
                1234L,
                Periode(
                    periodeId,
                    identitetsnummer,
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            identitetsnummer,
                            "idporten-loa-high"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )
            topologyTestDriver.advanceWallClockTime(Duration.ofDays(21))
            hendelseKeyValueStore.get(periodeId) shouldNotBe null
        }
    }
})

fun verifyEmptyTopic(hendelseloggOutputTopic: TestOutputTopic<out Any, out Any>) {
    if (hendelseloggOutputTopic.isEmpty) return
    val records = hendelseloggOutputTopic.readKeyValuesToList()
        .map { it.key to it.value }
    fail(
        "Forventet at topic hendelseloggOutputTopic skulle være tom, følgende records ble funnet:\n ${
            records.toList().map { "$it\n" }
        }"
    )
}

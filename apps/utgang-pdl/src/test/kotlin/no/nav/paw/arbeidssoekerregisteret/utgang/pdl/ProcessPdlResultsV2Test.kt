package no.nav.paw.arbeidssoekerregisteret.utgang.pdl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import io.mockk.verify
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.filterAvsluttPeriodeGrunnlag
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.filterValidHendelseStates
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.getHendelseStateAndPerson
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.isPdlResultOK
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.processPdlResultsV2
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.processResults
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.HendelseState
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.toAarsak
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.toAarsak
import no.nav.paw.arbeidssokerregisteret.application.IkkeBosattINorgeIHenholdTilFolkeregisterloven
import no.nav.paw.arbeidssokerregisteret.application.InngangsReglerV3
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Foedsel
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult
import org.apache.kafka.streams.KeyValue
import org.slf4j.Logger
import java.time.Duration
import java.time.Instant
import java.util.*

class ProcessPdlResultsV2Test : FreeSpec({

    "processResults v1 and v2 should yield same results" {
        val logger = mockk<Logger>(relaxed = true)
        val prometheusMeterRegistry = mockk<PrometheusMeterRegistry>(relaxed = true)

        val ikkeBosattPerson = getPerson(
            foedsel = Foedsel("2000-01-01"),
            statsborgerskap = getStatsborgerskap("BRA"),
            opphold = null,
            folkeregisterpersonstatus = getFolkeregisterpersonstatus("ikkeBosatt"),
            bostedsadresse = null,
            innflyttingTilNorge = emptyList(),
            utflyttingFraNorge = emptyList()
        )

        val resultV1 = generatePdlMockResponse(
            "12345678911",
            listOf("ikkeBosatt"),
            "ok"
        )

        val resultV2 = HentPersonBolkResult(
            "12345678911",
            ikkeBosattPerson,
            "ok"
        )

        val hendelseState = HendelseState(
            brukerId = 1L,
            periodeId = UUID.randomUUID(),
            recordKey = 1L,
            identitetsnummer = "12345678911",
            opplysninger = setOf(
                Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                Opplysning.ER_OVER_18_AAR,
                Opplysning.IKKE_ANSATT,
                Opplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                Opplysning.INGEN_FLYTTE_INFORMASJON,
                Opplysning.INGEN_ADRESSE_FUNNET,
                Opplysning.DNUMMER
            ),
            startetTidspunkt = Instant.now().minus(Duration.ofDays(30)),
            harTilhoerendePeriode = true
        )

        val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

        val outputV1 = resultV1.processResults(chunk, prometheusMeterRegistry, logger)
        val outputV2 = listOf(resultV2).processPdlResultsV2(InngangsReglerV3, chunk, logger)

        outputV1.shouldHaveSize(1)
        outputV2.shouldHaveSize(1)

        outputV1[0].avsluttPeriode shouldBe outputV2[0].avsluttPeriode
        outputV1[0].slettForhaandsGodkjenning shouldBe outputV2[0].slettForhaandsGodkjenning
        outputV1[0].hendelseState shouldBe outputV2[0].hendelseState
        outputV1[0].grunnlagV1?.filterAvsluttPeriodeGrunnlag(hendelseState.opplysninger)?.toAarsak() shouldBe "Personen er ikke bosatt etter folkeregisterloven"
        outputV2[0].grunnlagV2 shouldBe setOf(IkkeBosattINorgeIHenholdTilFolkeregisterloven)
    }

    "processPdlResultsV2 should correctly set avsluttPeriode to true if multiple problems in pdlEvaluering" {
        val person = getPerson(
            foedsel = Foedsel("2014-01-01"),
            statsborgerskap = getStatsborgerskap("NOR"),
            opphold = null,
            folkeregisterpersonstatus = getFolkeregisterpersonstatus("ikkeBosatt"),
            bostedsadresse = null,
            innflyttingTilNorge = emptyList(),
            utflyttingFraNorge = emptyList()
        )

        val result = HentPersonBolkResult("12345678911", person, "ok")

        val hendelseState = HendelseState(
            brukerId = 1L,
            periodeId = UUID.randomUUID(),
            recordKey = 1L,
            identitetsnummer = "12345678911",
            opplysninger = setOf(
                Opplysning.ER_UNDER_18_AAR,
                Opplysning.BOSATT_ETTER_FREG_LOVEN,
                Opplysning.FORHAANDSGODKJENT_AV_ANSATT
            ),
            startetTidspunkt = Instant.now().minus(Duration.ofDays(30)),
            harTilhoerendePeriode = true
        )

        val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

        val logger = mockk<Logger>(relaxed = true)

        val output = listOf(result).processPdlResultsV2(InngangsReglerV3, chunk, logger)

        output.shouldHaveSize(1)
        output[0].avsluttPeriode shouldBe true

    }

    "processPdlResultsV2 should correctly set avsluttPeriode to true" - {

        "if Folkeregisterpersonstatus 'ikkeBosatt'" {
            val logger = mockk<Logger>(relaxed = true)

            val utflyttetPerson = getPerson(
                foedsel = Foedsel("2000-01-01"),
                statsborgerskap = getStatsborgerskap("BRA"),
                opphold = null,
                folkeregisterpersonstatus = getFolkeregisterpersonstatus("ikkeBosatt"),
                bostedsadresse = null,
                innflyttingTilNorge = emptyList(),
                utflyttingFraNorge = emptyList()
            )
            val result = HentPersonBolkResult("12345678911", utflyttetPerson, "ok")
            val hendelseState = HendelseState(
                brukerId = 1L,
                periodeId = UUID.randomUUID(),
                recordKey = 1L,
                identitetsnummer = "12345678911",
                opplysninger = setOf(Opplysning.HAR_UTENLANDSK_ADRESSE, Opplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE),
                startetTidspunkt = Instant.now().minus(Duration.ofDays(30)),
                harTilhoerendePeriode = true
            )
            val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

            val output = listOf(result).processPdlResultsV2(InngangsReglerV3, chunk, logger)

            output.shouldHaveSize(1)
            val evalueringResultat = output.first()

            evalueringResultat.hendelseState shouldBe hendelseState
            evalueringResultat.avsluttPeriode shouldBe true
            evalueringResultat.slettForhaandsGodkjenning shouldBe false
        }

        "if Folkeregisterpersonstatus 'doedIFolkeregisteret'" {
            val logger = mockk<Logger>(relaxed = true)

            val doedPerson = getPerson(
                foedsel = Foedsel("2006-01-01"),
                statsborgerskap = getStatsborgerskap("NOR"),
                opphold = null,
                folkeregisterpersonstatus = getFolkeregisterpersonstatus("doedIFolkeregisteret"),
                bostedsadresse = null,
                innflyttingTilNorge = emptyList(),
                utflyttingFraNorge = emptyList()
            )
            val result = HentPersonBolkResult("12345678911", doedPerson, "ok")
            val hendelseState = HendelseState(
                brukerId = 1L,
                periodeId = UUID.randomUUID(),
                recordKey = 1L,
                identitetsnummer = "12345678911",
                opplysninger = setOf(Opplysning.ER_OVER_18_AAR, Opplysning.ER_NORSK_STATSBORGER, Opplysning.HAR_NORSK_ADRESSE),
                startetTidspunkt = Instant.now().minus(Duration.ofDays(30)),
                harTilhoerendePeriode = true
            )
            val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

            val output = listOf(result).processPdlResultsV2(InngangsReglerV3, chunk, logger)

            output.shouldHaveSize(1)
            val evalueringResultat = output.first()

            evalueringResultat.hendelseState shouldBe hendelseState
            evalueringResultat.avsluttPeriode shouldBe true
            evalueringResultat.slettForhaandsGodkjenning shouldBe false
        }

        "if Folkeregisterpersonstatus 'forsvunnet'" {
            val logger = mockk<Logger>(relaxed = true)

            val savnetPerson = getPerson(
                foedsel = Foedsel("2006-01-01"),
                statsborgerskap = getStatsborgerskap("NOR"),
                opphold = null,
                folkeregisterpersonstatus = getFolkeregisterpersonstatus("forsvunnet"),
                bostedsadresse = null,
                innflyttingTilNorge = emptyList(),
                utflyttingFraNorge = emptyList()
            )
            val result = HentPersonBolkResult("12345678911", savnetPerson, "ok")
            val hendelseState = HendelseState(
                brukerId = 1L,
                periodeId = UUID.randomUUID(),
                recordKey = 1L,
                identitetsnummer = "12345678911",
                opplysninger = setOf(Opplysning.ER_OVER_18_AAR, Opplysning.ER_NORSK_STATSBORGER, Opplysning.HAR_NORSK_ADRESSE),
                startetTidspunkt = Instant.now().minus(Duration.ofDays(30)),
                harTilhoerendePeriode = true
            )
            val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

            val output = listOf(result).processPdlResultsV2(InngangsReglerV3, chunk, logger)

            output.shouldHaveSize(1)
            val evalueringResultat = output.first()

            evalueringResultat.hendelseState shouldBe hendelseState
            evalueringResultat.avsluttPeriode shouldBe true
            evalueringResultat.slettForhaandsGodkjenning shouldBe false
        }
    }

    "processPdlResultsV2 should correctly set avsluttPeriode to false and slettForhaandsGodkjenning to false" - {

        "if Folkeregisterpersonstatus is 'bosattEtterFolkeregisterloven'" {
            val logger = mockk<Logger>(relaxed = true)
            val bosattPerson = getPerson(
                foedsel = Foedsel("2000-01-01"),
                statsborgerskap = getStatsborgerskap("NOR"),
                opphold = getOppholdstillatelse(),
                folkeregisterpersonstatus = getFolkeregisterpersonstatus("bosattEtterFolkeregisterloven"),
                bostedsadresse = getBostedsadresse(),
                innflyttingTilNorge = emptyList(),
                utflyttingFraNorge = emptyList()
            )
            val result = HentPersonBolkResult("12345678911", bosattPerson, "ok")
            val hendelseState = HendelseState(
                brukerId = 1L,
                periodeId = UUID.randomUUID(),
                recordKey = 1L,
                identitetsnummer = "12345678911",
                opplysninger = setOf(Opplysning.ER_OVER_18_AAR, Opplysning.ER_NORSK_STATSBORGER, Opplysning.HAR_NORSK_ADRESSE),
                startetTidspunkt = Instant.now().minus(Duration.ofDays(30)),
                harTilhoerendePeriode = true
            )
            val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

            val output = listOf(result).processPdlResultsV2(InngangsReglerV3, chunk, logger)

            output.shouldHaveSize(1)
            val evalueringResultat = output.first()

            evalueringResultat.hendelseState shouldBe hendelseState
            evalueringResultat.avsluttPeriode shouldBe false
            evalueringResultat.slettForhaandsGodkjenning shouldBe false
        }
        "if negative opplysning from pdl matches opplysning in hendelsestate and is forhaandsgodkjent" {
            val logger = mockk<Logger>(relaxed = true)

            val savnetPerson = getPerson(
                foedsel = Foedsel("2000-01-01"),
                statsborgerskap = getStatsborgerskap("NOR"),
                opphold = getOppholdstillatelse(),
                folkeregisterpersonstatus = getFolkeregisterpersonstatus("forsvunnet"),
                bostedsadresse = getBostedsadresse(),
                innflyttingTilNorge = emptyList(),
                utflyttingFraNorge = emptyList()
            )

            val result = HentPersonBolkResult("12345678911", savnetPerson, "ok")
            val hendelseState = HendelseState(
                brukerId = 1L,
                periodeId = UUID.randomUUID(),
                recordKey = 1L,
                identitetsnummer = "12345678911",
                opplysninger = setOf(Opplysning.SAVNET, Opplysning.FORHAANDSGODKJENT_AV_ANSATT),
                startetTidspunkt = Instant.now().minus(Duration.ofDays(30)),
                harTilhoerendePeriode = true
            )
            val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

            val output = listOf(result).processPdlResultsV2(InngangsReglerV3, chunk, logger)

            output.shouldHaveSize(1)
            val evalueringResultat = output.first()

            evalueringResultat.hendelseState shouldBe hendelseState
            evalueringResultat.avsluttPeriode shouldBe false
            evalueringResultat.slettForhaandsGodkjenning shouldBe false
        }
    }

    "processPdlResultsV2 should correctly set slettForhaandsGodkjenning to true" - {

        "if has negative opplysning and is forhaandsgodkjent and PDL gives positive results" {
            val logger = mockk<Logger>(relaxed = true)

            val bosattPerson = getPerson(
                foedsel = Foedsel("2000-01-01"),
                statsborgerskap = getStatsborgerskap("NOR"),
                opphold = getOppholdstillatelse(),
                folkeregisterpersonstatus = getFolkeregisterpersonstatus("bosattEtterFolkeregisterloven"),
                bostedsadresse = getBostedsadresse(),
                innflyttingTilNorge = emptyList(),
                utflyttingFraNorge = emptyList()
            )
            val result = HentPersonBolkResult("12345678911", bosattPerson, "ok")
            val hendelseState = HendelseState(
                brukerId = 1L,
                periodeId = UUID.randomUUID(),
                recordKey = 1L,
                identitetsnummer = "12345678911",
                opplysninger = setOf(Opplysning.SAVNET, Opplysning.FORHAANDSGODKJENT_AV_ANSATT),
                startetTidspunkt = Instant.now().minus(Duration.ofDays(30)),
                harTilhoerendePeriode = true
            )
            val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

            val output = listOf(result).processPdlResultsV2(InngangsReglerV3, chunk, logger)

            output.shouldHaveSize(1)
            val evalueringResultat = output.first()

            evalueringResultat.hendelseState shouldBe hendelseState
            evalueringResultat.avsluttPeriode shouldBe false
            evalueringResultat.slettForhaandsGodkjenning shouldBe true
        }
    }

    "processPdlResultsV2 should correctly evaluate negative pdl results" - {

        "should return an empty list when all results have error codes" {
            val logger = mockk<Logger>(relaxed = true)

            val results = listOf(
                HentPersonBolkResult("12345678901", null, "bad_request"),
                HentPersonBolkResult("12345678902", null, "not_found")
            )

            val chunk = listOf<KeyValue<UUID, HendelseState>>()

            val output = results.processPdlResultsV2(InngangsReglerV3, chunk, logger)

            output.shouldBeEmpty()
            verify(exactly = 2) { logger.error(any()) }
        }

        "should return an empty list and log an error when the person is null" {
            val logger = mockk<Logger>(relaxed = true)

            val result = HentPersonBolkResult("12345678901", null, "ok")
            val periodeId = UUID.randomUUID()
            val chunk = listOf(
                KeyValue(
                    periodeId,
                    HendelseState(
                        1L,
                        periodeId,
                        1L,
                        "12345678901",
                        emptySet(),
                        Instant.now(),
                        true
                    )
                )
            )

            val output = listOf(result).processPdlResultsV2(InngangsReglerV3, chunk, logger)

            output.shouldBeEmpty()
            verify { logger.error("Person er null for periodeId: $periodeId") }
        }
    }

    "isPdlResultOK should correctly log and return false for error codes" - {
        "should return false and log an error when the code is in pdlErrorResponses" {
            val logger = mockk<Logger>(relaxed = true)
            val result = isPdlResultOK("bad_request", logger)
            result shouldBe false
            verify { logger.error("Feil ved henting av Person fra PDL: bad_request") }
        }

        "should return true and not log an error for a valid code" {
            val logger = mockk<Logger>(relaxed = true)
            val result = isPdlResultOK("ok", logger)
            result shouldBe true
            verify(exactly = 0) { logger.error(any()) }
        }
    }

    "getHendelseStateAndPerson should return null if person is null" {
        val logger = mockk<Logger>(relaxed = true)
        val result = HentPersonBolkResult("12345678911", null, "ok")
        val periodeId = UUID.randomUUID()
        val chunk = listOf<KeyValue<UUID, HendelseState>>(
            KeyValue(
                periodeId,
                HendelseState(
                    1L,
                    periodeId,
                    1L,
                    "12345678911",
                    emptySet(),
                    Instant.now(),
                    true
                )
            )
        )

        val output = getHendelseStateAndPerson(result, chunk, logger)

        output.shouldBeNull()
        verify { logger.error("Person er null for periodeId: $periodeId") }
    }

    "getHendelseStateAndPerson should correctly map a valid result" {
        val logger = mockk<Logger>(relaxed = true)

        val validPerson = getPerson(
            foedsel = Foedsel("2000-01-01"),
            statsborgerskap = getStatsborgerskap("NOR"),
            opphold = getOppholdstillatelse(),
            folkeregisterpersonstatus = getFolkeregisterpersonstatus("bosatt"),
            bostedsadresse = getBostedsadresse(),
            innflyttingTilNorge = emptyList(),
            utflyttingFraNorge = emptyList()
        )
        val result = HentPersonBolkResult("12345678911", validPerson, "ok")
        val hendelseState = HendelseState(
            brukerId = 1L,
            periodeId = UUID.randomUUID(),
            recordKey = 1L,
            identitetsnummer = "12345678911",
            opplysninger = setOf(Opplysning.ER_NORSK_STATSBORGER),
            startetTidspunkt = Instant.now(),
            harTilhoerendePeriode = true
        )
        val chunk = listOf(KeyValue(hendelseState.periodeId, hendelseState))

        val output = getHendelseStateAndPerson(result, chunk, logger)

        output.shouldNotBeNull()
        output.first shouldBe validPerson
        output.second shouldBe hendelseState
    }

    "filterValidHendelseStates should correctly filter out invalid states" {
        val validHendelseState = HendelseState(
            brukerId = 1L,
            periodeId = UUID.randomUUID(),
            recordKey = 1L,
            identitetsnummer = "12345",
            opplysninger = setOf(Opplysning.ER_NORSK_STATSBORGER),
            startetTidspunkt = Instant.now(),
            harTilhoerendePeriode = true
        )
        val invalidHendelseState = HendelseState(
            brukerId = null,
            periodeId = UUID.randomUUID(),
            recordKey = 2L,
            identitetsnummer = "67890",
            opplysninger = setOf(Opplysning.ER_NORSK_STATSBORGER),
            startetTidspunkt = Instant.now(),
            harTilhoerendePeriode = false // This should cause it to be filtered out
        )

        val states = listOf(
            KeyValue(UUID.randomUUID(), validHendelseState),
            KeyValue(UUID.randomUUID(), invalidHendelseState)
        )
        val filteredStates = states.filterValidHendelseStates()

        filteredStates.shouldHaveSize(1)
        filteredStates.first().value shouldBe validHendelseState
    }
})

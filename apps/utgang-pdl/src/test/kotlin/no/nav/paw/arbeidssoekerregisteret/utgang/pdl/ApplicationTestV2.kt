package no.nav.paw.arbeidssoekerregisteret.utgang.pdl

/*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker as InternBruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType as InternBrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata as InternMetadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import ch.qos.logback.classic.Logger as LogbackLogger

class ApplicationTestV2 : FreeSpec({
    val periodeId = UUID.randomUUID()



    "Sender ikke AvsluttetHendelse og logger error for PersonIkkeFunnet" {
        val logger = LoggerFactory.getLogger("scheduleAvsluttPerioder") as LogbackLogger
        val testAppender = TestAppender()
        logger.addAppender(testAppender)
        testAppender.start()
        with(
            testScopeV2(
                generatePdlHentPersonMockResponse(
                    "12345678901",
                    null,
                    "PersonIkkeFunnet"
                )
            )
        ) {
            verifyEmptyTopic(hendelseloggOutputTopic)
            hendelseloggInputTopic.pipeInput(1234L,
                Startet(
                    periodeId,
                    1234L,
                    "12345678901",
                    InternMetadata(
                        Instant.now(),
                        InternBruker(
                            InternBrukerType.SLUTTBRUKER,
                            "12345678901"
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
                    "12345678901",
                    Metadata(
                        Instant.now(),
                        Bruker(
                            BrukerType.SLUTTBRUKER,
                            "12345678901"
                        ),
                        "",
                        "",
                        null
                    ),
                    null
                )
            )

            topologyTestDriver.advanceWallClockTime(Duration.ofDays(2))
            hendelseloggOutputTopic.isEmpty shouldBe true
            val logEvents = testAppender.events
            logEvents.any {
                it.level == ch.qos.logback.classic.Level.ERROR
                && it.message.contains("Versjon 2: Person er null for periodeId: $periodeId")
            } shouldBe true
        }
        testAppender.stop()
    }
})
*/

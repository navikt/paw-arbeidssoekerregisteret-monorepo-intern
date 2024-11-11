package no.nav.paw.kafkakeymaintenance

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad
import no.nav.paw.kafkakeymaintenance.perioder.insertOrUpdate
import no.nav.paw.kafkakeymaintenance.perioder.periodeRad
import no.nav.paw.test.minutes
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class PeriodeRadTest: FreeSpec ({
    val logger = LoggerFactory.getLogger("test-logger")
    "Test periode rad db funksjoner" - {
        initDbContainer()
        val txCtx = txContext(
            ApplicationContext(
                consumerVersion = 1,
                logger = logger,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
            )
        )
        val rad1 = PeriodeRad(
            periodeId = UUID.randomUUID(),
            identitetsnummer = "12345678901",
            fra = Instant.now().truncatedTo(ChronoUnit.MICROS),
            til = null
        )
        val rad2 = PeriodeRad(
            periodeId = UUID.randomUUID(),
            identitetsnummer = "12345678902",
            fra = Instant.now().truncatedTo(ChronoUnit.MICROS),
            til = Instant.now().truncatedTo(ChronoUnit.MICROS) + 2.minutes
        )
        val rad3 = rad1.copy(til = Instant.now().truncatedTo(ChronoUnit.MICROS) + 3.minutes)
        "Vi kan lagre og hente $rad1" {
            transaction {
                with(txCtx()) {
                    insertOrUpdate(rad1)
                    periodeRad(rad1.identitetsnummer) shouldBe rad1
                }
            }
        }
        "Vi takler duplikate lagring av $rad1" {
            transaction {
                with(txCtx()) {
                    insertOrUpdate(rad1)
                    periodeRad(rad1.identitetsnummer) shouldBe rad1
                }
            }
        }
        "Vi kan oppdatere lagre og hente $rad2" {
            transaction {
                with(txCtx()) {
                    insertOrUpdate(rad2)
                    periodeRad(rad2.identitetsnummer) shouldBe rad2
                }
            }
        }
        "Vi kan oppdatere $rad1 til $rad3" {
            transaction {
                with(txCtx()) {
                    insertOrUpdate(rad3)
                    periodeRad(rad3.identitetsnummer) shouldBe rad3
                    periodeRad(rad1.identitetsnummer) shouldBe rad3
                }
            }
        }
    }
})